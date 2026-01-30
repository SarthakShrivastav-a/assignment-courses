package api.assignment.backend.dto.search;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SearchResponse {
    private String query;
    private List<CourseSearchResult> results;
}
