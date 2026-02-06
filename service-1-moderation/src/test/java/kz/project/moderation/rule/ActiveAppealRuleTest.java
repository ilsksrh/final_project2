package kz.project.moderation.rule;

import kz.project.moderation.dto.AppealEvent;
import kz.project.moderation.repository.AppealRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActiveAppealRuleTest {

    @Mock
    private AppealRepository appealRepository;

    private ActiveAppealRule rule;

    @BeforeEach
    void setUp() {
        rule = new ActiveAppealRule(appealRepository);
    }

    @Test
    void shouldRejectIfActiveAppealExistsForCategory() {
        AppealEvent event = new AppealEvent();
        event.setClientId(123L);
        event.setCategory("urgent");

        when(appealRepository.existsByClientIdAndCategoryAndStatus(
                eq(123L), eq("urgent"), eq("ACTIVE"))).thenReturn(true);

        assertFalse(rule.check(event, null));
    }

    @Test
    void shouldAllowIfNoActiveAppealForCategory() {
        AppealEvent event = new AppealEvent();
        event.setClientId(123L);
        event.setCategory("support");

        when(appealRepository.existsByClientIdAndCategoryAndStatus(
                any(), anyString(), eq("ACTIVE"))).thenReturn(false);

        assertTrue(rule.check(event, null));
    }
}