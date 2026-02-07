package kz.project.moderation.service;

import kz.project.moderation.dto.AppealEvent;
import kz.project.moderation.dto.EnrichmentResponse;
import kz.project.moderation.repository.AppealRepository;
import kz.project.moderation.repository.ProcessedEventRepository;
import kz.project.moderation.rule.ModerationRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.reactive.function.client.WebClient;

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

    @Mock
    private WebClient enrichmentWebClient;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate; // Тип Object для универсальности в тесте

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private AppealRepository appealRepository;

    private ModerationService service;

    @BeforeEach
    void setUp() {
        // В конструкторе ModerationService 6 полей:
        // 1. rules, 2. webClient, 3. kafka, 4. processedRepo, 5. appealRepo, 6. searchRepo
        service = new ModerationService(
                List.of(rule1, rule2),
                enrichmentWebClient,
                (KafkaTemplate) kafkaTemplate,
                processedEventRepository,
                appealRepository
        );
    }

    @Test
    void shouldApproveWhenAllRulesPass() {
        AppealEvent event = new AppealEvent();
        event.setEventId(UUID.randomUUID());

        EnrichmentResponse enrichment = new EnrichmentResponse();
        enrichment.setTotalActiveAppeals(0); // Важно задать, чтобы не было null

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
        enrichment.setTotalActiveAppeals(0);

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
        enrichment.setTotalActiveAppeals(6); // Порог в коде > 5

        String decision = service.applyRulesAndDecide(event, enrichment);

        assertEquals(
                "REJECTED: Too many active appeals",
                decision
        );
    }
}