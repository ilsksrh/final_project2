package kz.project.moderation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "processed_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEventEntity {

    @Id
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(
            name = "processed_at",
            nullable = false,
            columnDefinition = "timestamptz"
    )
    private OffsetDateTime processedAt;
}

