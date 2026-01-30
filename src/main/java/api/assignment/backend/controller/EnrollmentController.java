package api.assignment.backend.controller;

import api.assignment.backend.dto.enrollment.EnrollmentResponse;
import api.assignment.backend.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping("/api/courses/{courseId}/enroll")
    public ResponseEntity<EnrollmentResponse> enroll(@PathVariable String courseId) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(enrollmentService.enroll(email, courseId));
    }

}
