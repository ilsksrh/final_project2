package kz.project.moderation.repository;


import kz.project.moderation.entity.ProcessedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, UUID> {

    boolean existsByEventId(UUID eventId);
}

//проверки идемпотентности
//защиты от дублей при at-least-once delivery