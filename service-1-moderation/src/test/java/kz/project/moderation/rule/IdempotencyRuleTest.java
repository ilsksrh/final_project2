package kz.project.moderation.rule;

import kz.project.moderation.dto.AppealEvent;
import kz.project.moderation.repository.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyRuleTest {

    @Mock
    private ProcessedEventRepository processedEventRepository;

    private IdempotencyRule rule;

    @BeforeEach
    void setUp() {
        rule = new IdempotencyRule(processedEventRepository);
    }

    @Test
    void shouldRejectIfEventAlreadyProcessed() {
        AppealEvent event = new AppealEvent();
        event.setEventId(UUID.randomUUID());

        when(processedEventRepository.existsByEventId(any())).thenReturn(true);

        assertFalse(rule.check(event, null));
    }

    @Test
    void shouldAllowIfEventIsNew() {
        AppealEvent event = new AppealEvent();
        event.setEventId(UUID.randomUUID());

        when(processedEventRepository.existsByEventId(any())).thenReturn(false);

        assertTrue(rule.check(event, null));
    }
}