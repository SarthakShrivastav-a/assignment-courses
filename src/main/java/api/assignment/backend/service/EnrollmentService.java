package api.assignment.backend.service;

import api.assignment.backend.dto.enrollment.EnrollmentResponse;
import api.assignment.backend.entity.Course;
import api.assignment.backend.entity.Enrollment;
import api.assignment.backend.exception.DuplicateResourceException;
import api.assignment.backend.exception.ResourceNotFoundException;
import api.assignment.backend.repository.CourseRepository;
import api.assignment.backend.repository.EnrollmentRepository;
import api.assignment.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public EnrollmentResponse enroll(String email, String courseId) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Course with id '" + courseId + "' does not exist"));

        if (enrollmentRepository.existsByUserIdAndCourseId(user.getId(), courseId)) {
            throw new DuplicateResourceException("You are already enrolled in this course");
        }

        Enrollment enrollment = Enrollment.builder()
                .userId(user.getId())
                .courseId(courseId)
                .build();

        enrollment = enrollmentRepository.save(enrollment);

        return new EnrollmentResponse(
                enrollment.getId(),
                courseId,
                course.getTitle(),
                enrollment.getEnrolledAt()
        );
    }
}
