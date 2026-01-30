package api.assignment.backend.dto.enrollment;

import api.assignment.backend.dto.progress.CompletedItemDto;
import api.assignment.backend.dto.progress.TopicProgressDto;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ProgressResponse {
    private Long enrollmentId;
    private String courseId;
    private String courseTitle;
    private int totalSubtopics;
    private int completedSubtopics;
    private double completionPercentage;
    private List<TopicProgressDto> topicProgress;
    private List<CompletedItemDto> completedItems;
}
