package kz.project.moderation.dto;

import lombok.Data;

import java.util.List;

@Data
public class EnrichmentResponse {
    private List<String> activeCategories = List.of();
    private int totalActiveAppeals = 0;
}