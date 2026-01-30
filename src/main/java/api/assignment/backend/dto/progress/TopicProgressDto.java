package api.assignment.backend.dto.progress;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TopicProgressDto {
    private String topicId;
    private String topicTitle;
    private int totalSubtopics;
    private int completedSubtopics;
    private boolean completed;
}
