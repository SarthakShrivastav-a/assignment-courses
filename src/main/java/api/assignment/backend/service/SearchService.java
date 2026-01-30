package api.assignment.backend.service;

import api.assignment.backend.dto.search.*;
import api.assignment.backend.entity.Course;
import api.assignment.backend.entity.Subtopic;
import api.assignment.backend.entity.Topic;
import api.assignment.backend.repository.CourseRepository;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SearchService {

    private static final String INDEX_NAME = "course_content";

    @Nullable //why is this deprecated
    private final ElasticsearchClient esClient;
    private final CourseRepository courseRepository;

    private boolean esAvailable = false;

  // called by data loader after its done populating the data. (cannot rely on post construct for first time startup)
    public void initializeElasticsearch() {
        if (esClient == null) {
            log.info("Elasticsearch client is null — using PostgreSQL fallback for search.");
            return;
        }
        try {
            esClient.ping();
            esAvailable = true;
            log.info("Elasticsearch is available. Indexing course content...");
            indexAllCourses();
        } catch (Exception e) {
            log.warn("Elasticsearch not reachable: {}. Using PostgreSQL fallback.", e.getMessage());
        }
    }

    public SearchResponse search(String query) {
        if (esAvailable) {
            return elasticsearchSearch(query);
        }
        return postgresSearch(query);
    }

    private void indexAllCourses() {
        try {
            // delete the index then recreate
            try {
                esClient.indices().delete(d -> d.index(INDEX_NAME));
            } catch (Exception ignored) {}

            esClient.indices().create(c -> c.index(INDEX_NAME));

            List<Course> courses = courseRepository.findAll();
            if (courses.isEmpty()) return;

            var bulkBuilder = new BulkRequest.Builder();

            for (Course course : courses) {
                for (Topic topic : course.getTopics()) {
                    for (Subtopic subtopic : topic.getSubtopics()) {
                        Map<String, Object> doc = new HashMap<>();
                        doc.put("courseId", course.getId());
                        doc.put("courseTitle", course.getTitle());
                        doc.put("courseDescription", course.getDescription());
                        doc.put("topicTitle", topic.getTitle());
                        doc.put("subtopicId", subtopic.getId());
                        doc.put("subtopicTitle", subtopic.getTitle());
                        doc.put("content", subtopic.getContent());

                        bulkBuilder.operations(op -> op
                                .index(idx -> idx
                                        .index(INDEX_NAME)
                                        .id(subtopic.getId())
                                        .document(doc)
                                ));
                    }
                }
            }

            var bulkResponse = esClient.bulk(bulkBuilder.build());
            if (bulkResponse.errors()) {
                log.error("Errors during bulk indexing.");
            } else {
                log.info("Indexed course content into Elasticsearch successfully.");
            }
        } catch (Exception e) {
            log.error("Failed to index courses into Elasticsearch: {}", e.getMessage());
            esAvailable = false;
        }
    }

    // ---- Elasticsearch search ----

    private SearchResponse elasticsearchSearch(String query) {
        try {
            var searchRequest = SearchRequest.of(s -> s
                    .index(INDEX_NAME)
                    .query(q -> q
                            .multiMatch(mm -> mm
                                    .query(query)
                                    .fields(
                                            "courseTitle^3",
                                            "courseDescription^2",
                                            "topicTitle^3",
                                            "subtopicTitle^3",
                                            "content"
                                    )
                                    .fuzziness("AUTO")
                            )
                    )
                    .highlight(h -> h
                            .fields("courseTitle", HighlightField.of(hf -> hf))
                            .fields("topicTitle", HighlightField.of(hf -> hf))
                            .fields("subtopicTitle", HighlightField.of(hf -> hf))
                            .fields("content", HighlightField.of(hf -> hf
                                    .fragmentSize(150)
                                    .numberOfFragments(1)
                            ))
                    )
                    .size(50)
            );

            var response = esClient.search(searchRequest, Map.class);

            // group hit by course id
            Map<String, List<Hit<Map>>> byCourse = new LinkedHashMap<>();
            Map<String, String> courseTitles = new HashMap<>();

            for (Hit<Map> hit : response.hits().hits()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> source = hit.source();
                if (source == null) continue;

                String courseId = (String) source.get("courseId");
                byCourse.computeIfAbsent(courseId, k -> new ArrayList<>()).add(hit);
                courseTitles.putIfAbsent(courseId, (String) source.get("courseTitle"));
            }

            List<CourseSearchResult> results = byCourse.entrySet().stream()
                    .map(entry -> {
                        List<SearchMatch> matches = entry.getValue().stream()
                                .map(hit -> {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> src = hit.source();
                                    var highlights = hit.highlight();

                                    String snippet = "";
                                    String matchType = "content";

                                    if (highlights != null && !highlights.isEmpty()) {
                                        if (highlights.containsKey("subtopicTitle")) {
                                            matchType = "subtopic";
                                            snippet = String.join(" ", highlights.get("subtopicTitle"));
                                        } else if (highlights.containsKey("topicTitle")) {
                                            matchType = "topic";
                                            snippet = String.join(" ", highlights.get("topicTitle"));
                                        } else if (highlights.containsKey("content")) {
                                            matchType = "content";
                                            snippet = String.join(" ", highlights.get("content"));
                                        } else if (highlights.containsKey("courseTitle")) {
                                            matchType = "course";
                                            snippet = String.join(" ", highlights.get("courseTitle"));
                                        }
                                    }

                                    if (snippet.isEmpty() && src != null) {
                                        String content = (String) src.get("content");
                                        snippet = content != null && content.length() > 150
                                                ? content.substring(0, 150) + "..."
                                                : content;
                                    }

                                    return new SearchMatch(
                                            matchType,
                                            src != null ? (String) src.get("topicTitle") : "",
                                            src != null ? (String) src.get("subtopicId") : "",
                                            src != null ? (String) src.get("subtopicTitle") : "",
                                            snippet
                                    );
                                })
                                .toList();

                        return new CourseSearchResult(entry.getKey(), courseTitles.get(entry.getKey()), matches);
                    })
                    .toList();

            return new SearchResponse(query, results);
        } catch (Exception e) {
            log.error("Elasticsearch search failed: {}. Falling back to PostgreSQL.", e.getMessage());
            return postgresSearch(query);
        }
    }

    // postgre fallback search with LIKE query

    private SearchResponse postgresSearch(String query) {
        String pattern = "%" + query.toLowerCase() + "%";
        List<Course> allCourses = courseRepository.findAll();

        List<CourseSearchResult> results = allCourses.stream()
                .map(course -> {
                    List<SearchMatch> matches = new ArrayList<>();

                    // Check course title and description
                    if (course.getTitle().toLowerCase().contains(query.toLowerCase())) {
                        matches.add(new SearchMatch("course", null, null, null, course.getTitle()));
                    }
                    if (course.getDescription() != null && course.getDescription().toLowerCase().contains(query.toLowerCase())) {
                        matches.add(new SearchMatch("course", null, null, null, course.getDescription()));
                    }

                    // Check topics and subtopics
                    for (Topic topic : course.getTopics()) {
                        if (topic.getTitle().toLowerCase().contains(query.toLowerCase())) {
                            matches.add(new SearchMatch("topic", topic.getTitle(), null, null, topic.getTitle()));
                        }
                        for (Subtopic subtopic : topic.getSubtopics()) {
                            if (subtopic.getTitle().toLowerCase().contains(query.toLowerCase())) {
                                matches.add(new SearchMatch("subtopic", topic.getTitle(), subtopic.getId(), subtopic.getTitle(), subtopic.getTitle()));
                            }
                            if (subtopic.getContent() != null && subtopic.getContent().toLowerCase().contains(query.toLowerCase())) {
                                String content = subtopic.getContent();
                                int idx = content.toLowerCase().indexOf(query.toLowerCase());
                                int start = Math.max(0, idx - 50);
                                int end = Math.min(content.length(), idx + query.length() + 50);
                                String snippet = (start > 0 ? "..." : "") + content.substring(start, end) + (end < content.length() ? "..." : "");

                                matches.add(new SearchMatch("content", topic.getTitle(), subtopic.getId(), subtopic.getTitle(), snippet));
                            }
                        }
                    }

                    return matches.isEmpty() ? null : new CourseSearchResult(course.getId(), course.getTitle(), matches);
                })
                .filter(Objects::nonNull)
                .toList();

        return new SearchResponse(query, results);
    }
}
