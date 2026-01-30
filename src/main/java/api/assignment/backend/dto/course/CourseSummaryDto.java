package api.assignment.backend.dto.course;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CourseSummaryDto {
    private String id;
    private String title;
    private String description;
    private int topicCount;
    private int subtopicCount;
}
