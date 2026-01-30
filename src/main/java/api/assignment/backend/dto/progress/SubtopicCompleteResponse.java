package api.assignment.backend.dto.progress;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class SubtopicCompleteResponse {
    private String subtopicId;
    private boolean completed;
    private Instant completedAt;
}
