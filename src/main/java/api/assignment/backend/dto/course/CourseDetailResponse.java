package api.assignment.backend.dto.course;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CourseDetailResponse {
    private String id;
    private String title;
    private String description;
    private List<TopicDto> topics;
}
