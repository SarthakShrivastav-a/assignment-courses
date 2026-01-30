package api.assignment.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "subtopic_progress", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "subtopic_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubtopicProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "subtopic_id", nullable = false)
    private String subtopicId;

    @Builder.Default
    private Instant completedAt = Instant.now();
}
