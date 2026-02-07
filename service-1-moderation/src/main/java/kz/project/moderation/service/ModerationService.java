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
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import org.springframework.web.reactive.function.client.WebClient;
import io.github.resilience4j.retry.annotation.Retry;

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

    public Mono<Void> process(AppealEvent event) {
        return Mono.fromCallable(() ->
                        processedEventRepository.existsByEventId(event.getEventId()))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(exists -> {
                    if (exists) {
                        log.info("Duplicate event skipped: {}", event.getEventId());
                        return Mono.empty();
                    }
                    return runModerationFlow(event);
                });
    }

    private Mono<Void> runModerationFlow(AppealEvent event) {
        return enrich(event.getClientId())
                .flatMap(enrichment -> {
                    String decision = applyRulesAndDecide(event, enrichment);
                    if ("APPROVED".equals(decision)) {
                        return Mono.fromRunnable(() -> approveAndPublish(event))
                                .subscribeOn(Schedulers.boundedElastic())
                                .then();
                    }
                    log.info("Event rejected: eventId={}, reason={}", event.getEventId(), decision);
                    return Mono.empty();
                })
                .then();
    }

    @Retry(name = "enrichment")
    private Mono<EnrichmentResponse> enrich(Long clientId) {
        return enrichmentWebClient.get()
                .uri("/extended-info/{clientId}", clientId)
                .retrieve()
                .bodyToMono(EnrichmentResponse.class)
                .timeout(Duration.ofSeconds(3))
                .defaultIfEmpty(new EnrichmentResponse())
                .onErrorResume(e -> {
                    log.warn("Enrichment failed for client {}: {}", clientId, e.getMessage());
                    return Mono.just(new EnrichmentResponse());
                });
    }

    String applyRulesAndDecide(AppealEvent event, EnrichmentResponse enrichment) {
        if (enrichment.getTotalActiveAppeals() > 5) {
            return "REJECTED: Too many active appeals";
        }

        for (ModerationRule rule : rules) {
            if (!rule.check(event, enrichment)) {
                return "REJECTED: " + rule.getRejectReason();
            }
        }
        return "APPROVED";
    }

    private void approveAndPublish(AppealEvent event) {
        if (processedEventRepository.existsByEventId(event.getEventId())) {
            return;
        }

        processedEventRepository.save(
                new ProcessedEventEntity(
                        event.getEventId(),
                        event.getClientId(),
                        OffsetDateTime.now()
                )
        );

        AppealEntity appeal = new AppealEntity();
        appeal.setClientId(event.getClientId());
        appeal.setCategory(event.getCategory());
        appeal.setTheme(event.getTheme() != null ? event.getTheme() : "No theme");
        appeal.setStatus("ACTIVE");
        appeal.setCreatedAt(event.getCreatedAt() != null ? event.getCreatedAt() : OffsetDateTime.now());
        appealRepository.save(appeal);

        ModerationResult result = new ModerationResult(
                event.getEventId(),
                event.getClientId(),
                event.getCategory(),
                event.getTheme(),
                "APPROVED",
                null,
                OffsetDateTime.now()
        );

        kafkaTemplate.send("topic-2", event.getEventId().toString(), result);
    }
}
