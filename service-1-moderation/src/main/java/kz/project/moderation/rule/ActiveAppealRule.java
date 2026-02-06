package kz.project.moderation.rule;

import kz.project.moderation.dto.AppealEvent;
import kz.project.moderation.dto.EnrichmentResponse;
import kz.project.moderation.repository.AppealRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(2)
public class ActiveAppealRule implements ModerationRule {

    private final AppealRepository appealRepository;

    @Override
    public boolean check(AppealEvent event, EnrichmentResponse enrichment) {
        boolean hasActive = appealRepository.existsByClientIdAndCategoryAndStatus(
                event.getClientId(),
                event.getCategory(),
                "ACTIVE"
        );
        if (hasActive) {
            log.info("ActiveAppealRule → REJECTED: eventId={}, clientId={}, category={}",
                    event.getEventId(), event.getClientId(), event.getCategory());
        }
        return !hasActive;
    }

    @Override
    public String getRejectReason() {

        return "Client already has an active appeal for this category";

    }
}