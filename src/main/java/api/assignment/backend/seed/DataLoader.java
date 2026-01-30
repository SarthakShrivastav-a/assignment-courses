package api.assignment.backend.seed;

import api.assignment.backend.entity.Course;
import api.assignment.backend.entity.Subtopic;
import api.assignment.backend.entity.Topic;
import api.assignment.backend.repository.CourseRepository;
import api.assignment.backend.service.SearchService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final CourseRepository courseRepository;
    private final ObjectMapper objectMapper;
    private final SearchService searchService;

    @Override
    public void run(String... args) {
        if (courseRepository.count() > 0) {
            log.info("Database already has courses — skipping seed.");
            return;
        }

        try {
            JsonNode root = loadJson();
            JsonNode coursesNode = root.get("courses");

            List<Course> courses = new ArrayList<>();
            for (JsonNode courseNode : coursesNode) {
                Course course = parseCourse(courseNode);
                courses.add(course);
            }

            courseRepository.saveAll(courses);
            log.info("Seeded {} courses into the database.", courses.size());
        } catch (Exception e) {
            log.error("Failed to seed data: {}", e.getMessage(), e);
        }

        // indexing after data population
        searchService.initializeElasticsearch();
    }

    private JsonNode loadJson() throws Exception {
        try {
            InputStream is = new ClassPathResource("seed_data/courses.json").getInputStream();
            return objectMapper.readTree(is);
        } catch (Exception e) {
            File file = new File("seed_data/courses.json");
            return objectMapper.readTree(file);
        }
    }

    private Course parseCourse(JsonNode node) {
        Course course = Course.builder()
                .id(node.get("id").asText())
                .title(node.get("title").asText())
                .description(node.get("description").asText())
                .build();

        int topicOrder = 0;
        for (JsonNode topicNode : node.get("topics")) {
            Topic topic = parseTopic(topicNode, course, topicOrder++);
            course.getTopics().add(topic);
        }
        return course;
    }

    private Topic parseTopic(JsonNode node, Course course, int order) {
        Topic topic = Topic.builder()
                .id(node.get("id").asText())
                .title(node.get("title").asText())
                .orderIndex(order)
                .course(course)
                .build();

        int subtopicOrder = 0;
        for (JsonNode subNode : node.get("subtopics")) {
            Subtopic subtopic = Subtopic.builder()
                    .id(subNode.get("id").asText())
                    .title(subNode.get("title").asText())
                    .content(subNode.get("content").asText())
                    .orderIndex(subtopicOrder++)
                    .topic(topic)
                    .build();
            topic.getSubtopics().add(subtopic);
        }
        return topic;
    }
}
