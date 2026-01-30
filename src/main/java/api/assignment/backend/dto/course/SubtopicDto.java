package api.assignment.backend.dto.course;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SubtopicDto {
    private String id;
    private String title;
    private String content;
}
