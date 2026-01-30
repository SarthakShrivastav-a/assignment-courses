package api.assignment.backend.dto.search;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CourseSearchResult {
    private String courseId;
    private String courseTitle;
    private List<SearchMatch> matches;
}
