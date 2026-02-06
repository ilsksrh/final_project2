package kz.project.moderation.rule;

import kz.project.moderation.dto.AppealEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkingHoursRuleTest {

    private WorkingHoursRule rule;

    @BeforeEach
    void setUp() {
        rule = new WorkingHoursRule();
    }

    @Test
    void shouldAllowDuringWorkingHours() {
        AppealEvent event = new AppealEvent();
        event.setEventId(UUID.randomUUID());
        event.setClientId(123L);
        event.setCategory("urgent");
        event.setCreatedAt(OffsetDateTime.of(2026, 2, 6, 10, 0, 0, 0, ZoneOffset.UTC));

        assertTrue(rule.check(event, null));
    }

    @Test
    void shouldRejectOutsideWorkingHours() {
        AppealEvent event = new AppealEvent();
        event.setEventId(UUID.randomUUID());
        event.setClientId(123L);
        event.setCategory("urgent");

        event.setCreatedAt(
                ZonedDateTime.of(
                        2026, 2, 6,
                        8, 0, 0, 0,
                        ZoneId.of("Asia/Almaty")
                ).toOffsetDateTime()
        );

        assertFalse(rule.check(event, null));
    }


    @Test
    void shouldAllowNonRestrictedCategory() {
        AppealEvent event = new AppealEvent();
        event.setEventId(UUID.randomUUID());
        event.setClientId(123L);
        event.setCategory("support");
        event.setCreatedAt(OffsetDateTime.of(2026, 2, 6, 3, 0, 0, 0, ZoneOffset.UTC));

        assertTrue(rule.check(event, null));
    }
}