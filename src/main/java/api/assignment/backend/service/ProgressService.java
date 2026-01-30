package api.assignment.backend.service;

import api.assignment.backend.dto.enrollment.ProgressResponse;
import api.assignment.backend.dto.progress.CompletedItemDto;
import api.assignment.backend.dto.progress.SubtopicCompleteResponse;
import api.assignment.backend.dto.progress.TopicProgressDto;
import api.assignment.backend.entity.Course;
import api.assignment.backend.entity.Enrollment;
import api.assignment.backend.entity.Subtopic;
import api.assignment.backend.entity.SubtopicProgress;
import api.assignment.backend.exception.NotEnrolledException;
import api.assignment.backend.exception.ResourceNotFoundException;
import api.assignment.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final SubtopicProgressRepository progressRepository;
    private final SubtopicRepository subtopicRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public SubtopicCompleteResponse markComplete(String email, String subtopicId) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Subtopic subtopic = subtopicRepository.findById(subtopicId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Subtopic with id '" + subtopicId + "' does not exist"));

        String courseId = subtopic.getTopic().getCourse().getId();

        if (!enrollmentRepository.existsByUserIdAndCourseId(user.getId(), courseId)) {
            throw new NotEnrolledException(
                    "You must be enrolled in this course to mark subtopics as complete");
        }

        // Idempotent — return existing if already completed
        SubtopicProgress progress = progressRepository
                .findByUserIdAndSubtopicId(user.getId(), subtopicId)
                .orElseGet(() -> progressRepository.save(
                        SubtopicProgress.builder()
                                .userId(user.getId())
                                .subtopicId(subtopicId)
                                .build()
                ));

        return new SubtopicCompleteResponse(subtopicId, true, progress.getCompletedAt());
    }

    public ProgressResponse getProgress(String email, Long enrollmentId) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Enrollment with id '" + enrollmentId + "' does not exist"));

        if (!enrollment.getUserId().equals(user.getId())) {
            throw new NotEnrolledException("You do not have access to this enrollment");
        }

        Course course = courseRepository.findById(enrollment.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        List<String> allSubtopicIds = course.getTopics().stream()
                .flatMap(t -> t.getSubtopics().stream())
                .map(Subtopic::getId)
                .toList();

        int totalSubtopics = allSubtopicIds.size();

        List<SubtopicProgress> completed = progressRepository
                .findAllByUserIdAndSubtopicIdIn(user.getId(), allSubtopicIds);

        Set<String> completedIds = completed.stream()
                .map(SubtopicProgress::getSubtopicId)
                .collect(Collectors.toSet());

        List<CompletedItemDto> completedItems = completed.stream()
                .map(p -> {
                    String title = subtopicRepository.findById(p.getSubtopicId())
                            .map(Subtopic::getTitle).orElse("Unknown");
                    return new CompletedItemDto(p.getSubtopicId(), title, p.getCompletedAt());
                })
                .toList();

        List<TopicProgressDto> topicProgress = course.getTopics().stream()
                .map(topic -> {
                    int topicTotal = topic.getSubtopics().size();
                    int topicCompleted = (int) topic.getSubtopics().stream()
                            .filter(s -> completedIds.contains(s.getId()))
                            .count();
                    return new TopicProgressDto(
                            topic.getId(),
                            topic.getTitle(),
                            topicTotal,
                            topicCompleted,
                            topicTotal > 0 && topicCompleted == topicTotal
                    );
                })
                .toList();

        double percentage = totalSubtopics == 0 ? 0
                : Math.round((double) completed.size() / totalSubtopics * 10000.0) / 100.0;

        return new ProgressResponse(
                enrollmentId,
                course.getId(),
                course.getTitle(),
                totalSubtopics,
                completed.size(),
                percentage,
                topicProgress,
                completedItems
        );
    }
}
