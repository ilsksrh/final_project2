package kz.project.moderation.repository;

import kz.project.moderation.entity.AppealEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppealRepository extends JpaRepository<AppealEntity, Long> {
    boolean existsByClientIdAndCategoryAndStatus(
            Long clientId,
            String category,
            String status
    );
}
