package api.assignment.backend.dto.progress;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class CompletedItemDto {
    private String subtopicId;
    private String subtopicTitle;
    private Instant completedAt;
}
