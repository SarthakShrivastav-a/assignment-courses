package api.assignment.backend.controller;

import api.assignment.backend.dto.enrollment.ProgressResponse;
import api.assignment.backend.dto.progress.SubtopicCompleteResponse;
import api.assignment.backend.service.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    @PostMapping("/api/subtopics/{subtopicId}/complete")
    public ResponseEntity<SubtopicCompleteResponse> markComplete(@PathVariable String subtopicId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(progressService.markComplete(email, subtopicId));
    }

    @GetMapping("/api/enrollments/{enrollmentId}/progress")
    public ResponseEntity<ProgressResponse> getProgress(@PathVariable Long enrollmentId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(progressService.getProgress(email, enrollmentId));
    }
}
