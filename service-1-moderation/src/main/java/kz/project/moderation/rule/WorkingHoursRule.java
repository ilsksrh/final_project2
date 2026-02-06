package kz.project.moderation.rule;

import kz.project.moderation.dto.AppealEvent;
import kz.project.moderation.dto.EnrichmentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;

@Slf4j
@Component
@Order(3)
public class WorkingHoursRule implements ModerationRule {
    private static final Set<String> RESTRICTED_CATEGORIES = Set.of("urgent", "credit", "loan");
    private static final LocalTime WORK_START = LocalTime.of(9, 0);
    private static final LocalTime WORK_END = LocalTime.of(18, 0);

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Almaty");

    @Override
    public boolean check(AppealEvent event, EnrichmentResponse enrichment) {
        String category = event.getCategory();
        if (category == null || !RESTRICTED_CATEGORIES.contains(category.toLowerCase())) {
            return true;
        }

        LocalTime eventTime = event.getCreatedAt() != null
                ? event.getCreatedAt()
                .atZoneSameInstant(BUSINESS_ZONE)
                .toLocalTime()
                : LocalTime.now(BUSINESS_ZONE);

        boolean inWorkingHours =
                !eventTime.isBefore(WORK_START) && eventTime.isBefore(WORK_END);

        if (!inWorkingHours) {
            log.info(
                    "WorkingHoursRule → REJECTED: eventId={}, clientId={}, category={}, time={}, zone={}",
                    event.getEventId(),
                    event.getClientId(),
                    category,
                    eventTime,
                    BUSINESS_ZONE
            );
        }

        return inWorkingHours;
    }


    @Override
    public String getRejectReason() {
        return "Appeal was received outside working hours (09:00–18:00) for restricted categories";

    }
}