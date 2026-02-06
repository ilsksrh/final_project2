package kz.project.moderation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "appeals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AppealEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false, length = 255)
    private String theme;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(
            name = "created_at",
            nullable = false,
            columnDefinition = "timestamptz"
    )
    private OffsetDateTime createdAt;
}
