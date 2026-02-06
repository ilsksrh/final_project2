package kz.project.moderation.service;

import kz.project.moderation.dto.AppealEvent;
import kz.project.moderation.dto.EnrichmentResponse;
import kz.project.moderation.rule.ModerationRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModerationServiceTest {

    @Mock
    private ModerationRule rule1;

    @Mock
    private ModerationRule rule2;

    private ModerationService service;

    @BeforeEach
    void setUp() {
        service = new ModerationService(List.of(rule1, rule2), null, null, null, null);
    }

    @Test
    void shouldApproveWhenAllRulesPass() {
        AppealEvent event = new AppealEvent();
        event.setEventId(UUID.randomUUID());

        EnrichmentResponse enrichment = new EnrichmentResponse();

        when(rule1.check(any(), any())).thenReturn(true);
        when(rule2.check(any(), any())).thenReturn(true);

        String decision = service.applyRulesAndDecide(event, enrichment);

        assertEquals("APPROVED", decision);
    }

    @Test
    void shouldRejectWhenFirstRuleFails() {
        AppealEvent event = new AppealEvent();
        event.setEventId(UUID.randomUUID());

        EnrichmentResponse enrichment = new EnrichmentResponse();

        when(rule1.check(any(), any())).thenReturn(false);
        when(rule1.getRejectReason()).thenReturn("Test reject reason");

        String decision = service.applyRulesAndDecide(event, enrichment);

        assertEquals("REJECTED: Test reject reason", decision);
    }

    @Test
    void shouldRejectByRedisCountWhenTooManyActiveAppeals() {
        AppealEvent event = new AppealEvent();
        event.setEventId(UUID.randomUUID());
        event.setClientId(123L);

        EnrichmentResponse enrichment = new EnrichmentResponse();
        enrichment.setTotalActiveAppeals(6);

        String decision = service.applyRulesAndDecide(event, enrichment);

        assertEquals(
                "REJECTED: Too many active appeals according to enrichment data (>5)",
                decision
        );
    }
}
