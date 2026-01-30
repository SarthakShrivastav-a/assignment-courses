package api.assignment.backend.dto.course;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CourseListResponse {
    private List<CourseSummaryDto> courses;
}
