package kz.project.moderation.rule;

import kz.project.moderation.dto.AppealEvent;
import kz.project.moderation.dto.EnrichmentResponse;

public interface ModerationRule {
    boolean check(AppealEvent event, EnrichmentResponse enrichment);
    String getRejectReason();
}