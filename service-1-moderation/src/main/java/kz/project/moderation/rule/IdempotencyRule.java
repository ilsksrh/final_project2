package kz.project.moderation.rule;

import kz.project.moderation.dto.AppealEvent;
import kz.project.moderation.dto.EnrichmentResponse;
import kz.project.moderation.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(1)
public class IdempotencyRule implements ModerationRule {

    private final ProcessedEventRepository processedEventRepository;

    @Override
    public boolean check(AppealEvent event, EnrichmentResponse enrichment) {
        boolean isDuplicate = processedEventRepository.existsByEventId(event.getEventId());
        if (isDuplicate) {
            log.info("IdempotencyRule → REJECTED: duplicate eventId={}, clientId={}",
                    event.getEventId(), event.getClientId());
        }
        return !isDuplicate;
    }

    @Override
    public String getRejectReason() {
        return "Duplicate event (already processed)";
    }
}