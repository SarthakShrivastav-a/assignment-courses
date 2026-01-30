package api.assignment.backend.service;

import api.assignment.backend.dto.course.*;
import api.assignment.backend.entity.Course;
import api.assignment.backend.exception.ResourceNotFoundException;
import api.assignment.backend.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;

    public CourseListResponse getAllCourses() {
        List<CourseSummaryDto> courses = courseRepository.findAll().stream()
                .map(c -> new CourseSummaryDto(
                        c.getId(),
                        c.getTitle(),
                        c.getDescription(),
                        c.getTopics().size(),
                        c.getTopics().stream().mapToInt(t -> t.getSubtopics().size()).sum()
                ))
                .toList();
        return new CourseListResponse(courses);
    }

    public CourseDetailResponse getCourseById(String courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Course with id '" + courseId + "' does not exist"));

        List<TopicDto> topics = course.getTopics().stream()
                .map(t -> new TopicDto(
                        t.getId(),
                        t.getTitle(),
                        t.getSubtopics().stream()
                                .map(s -> new SubtopicDto(s.getId(), s.getTitle(), s.getContent()))
                                .toList()
                ))
                .toList();

        return new CourseDetailResponse(course.getId(), course.getTitle(), course.getDescription(), topics);
    }
}
