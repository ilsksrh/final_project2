package kz.project.moderation.service;

import kz.project.moderation.dto.AppealEvent;
import kz.project.moderation.dto.EnrichmentResponse;
import kz.project.moderation.dto.ModerationResult;
import kz.project.moderation.entity.AppealEntity;
import kz.project.moderation.entity.ProcessedEventEntity;
import kz.project.moderation.repository.AppealRepository;
import kz.project.moderation.repository.ProcessedEventRepository;
import kz.project.moderation.rule.ModerationRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import io.github.resilience4j.retry.annotation.Retry;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModerationService {

    private final List<ModerationRule> rules;
    private final WebClient enrichmentWebClient;
    private final KafkaTemplate<String, ModerationResult> kafkaTemplate;
    private final ProcessedEventRepository processedEventRepository;
    private final AppealRepository appealRepository;

    public void process(AppealEvent event) {
        log.debug("Processing appeal event: eventId={}, clientId={}, category={}",
                event.getEventId(), event.getClientId(), event.getCategory());

        if (processedEventRepository.existsByEventId(event.getEventId())) {
            log.info("Duplicate event skipped (idempotency check): eventId={}", event.getEventId());
            return;
        }

        enrichAndProcess(event);
    }

    @Retry(name = "enrichment")
    private Mono<EnrichmentResponse> enrich(Long clientId) {
        return enrichmentWebClient
                .get()
                .uri("/extended-info/{clientId}", clientId)
                .retrieve()
                .bodyToMono(EnrichmentResponse.class)
                .timeout(Duration.ofSeconds(3))
                .defaultIfEmpty(new EnrichmentResponse())
                .doOnNext(resp -> log.debug("Enrichment received for client {}: activeCategories={}, totalActive={}",
                        clientId, resp.getActiveCategories(), resp.getTotalActiveAppeals()))
                .onErrorResume(e -> {
                    log.warn("Enrichment call failed for client {}: {}", clientId, e.getMessage());
                    return Mono.just(new EnrichmentResponse());
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    private void enrichAndProcess(AppealEvent event) {
        enrich(event.getClientId())
                .subscribe(
                        enrichment -> {
                            String decision = applyRulesAndDecide(event, enrichment);
                            if ("APPROVED".equals(decision)) {
                                approveAndPublish(event);
                            } else {
                                log.info("Event rejected: eventId={}, decision={}", event.getEventId(), decision);
                            }
                        },
                        error -> log.error("Enrichment processing failed for client {}, event {}: {}",
                                event.getClientId(), event.getEventId(), error.toString(), error)
                );
    }

    String applyRulesAndDecide(AppealEvent event, EnrichmentResponse enrichment) {
        if (enrichment.getTotalActiveAppeals() > 5) {
            log.info("Rejected by Redis enrichment: clientId={}, totalActiveAppeals={}",
                    event.getClientId(), enrichment.getTotalActiveAppeals());
            return "REJECTED: Too many active appeals according to enrichment data (>5)";

        }

        for (ModerationRule rule : rules) {
            if (!rule.check(event, enrichment)) {
                String reason = rule.getRejectReason();
                log.info("Rejected by rule {}: eventId={}, clientId={}, category={}, reason={}",
                        rule.getClass().getSimpleName(), event.getEventId(), event.getClientId(), event.getCategory(), reason);
                return "REJECTED: " + reason;
            }
        }
        return "APPROVED";
    }

    @Transactional
    public void approveAndPublish(AppealEvent event) {
        if (processedEventRepository.existsByEventId(event.getEventId())) {
            log.warn("Race condition detected: event already processed during approval: eventId={}", event.getEventId());
            return;
        }

        ProcessedEventEntity processed = new ProcessedEventEntity(
                event.getEventId(),
                event.getClientId(),
                OffsetDateTime.now()
        );
        processedEventRepository.save(processed);
        log.debug("Saved processed event: eventId={}", event.getEventId());

        AppealEntity appeal = new AppealEntity();
        appeal.setClientId(event.getClientId());
        appeal.setCategory(event.getCategory());
        appeal.setTheme(event.getTheme() != null ? event.getTheme() : "No theme");
        appeal.setStatus("ACTIVE");
        appeal.setCreatedAt(event.getCreatedAt() != null ? event.getCreatedAt() : OffsetDateTime.now());
        appealRepository.save(appeal);
        log.debug("Created active appeal for clientId={}, category={}", event.getClientId(), event.getCategory());

        ModerationResult result = new ModerationResult();
        result.setEventId(event.getEventId());
        result.setClientId(event.getClientId());
        result.setCategory(event.getCategory());
        result.setTheme(event.getTheme());
        result.setDecision("APPROVED");
        result.setRejectReason(null);
        result.setProcessedAt(OffsetDateTime.now());

        kafkaTemplate.send("topic-2", event.getEventId().toString(), result);
        log.info("Event fully approved and published to topic-2: eventId={}", event.getEventId());
    }
}