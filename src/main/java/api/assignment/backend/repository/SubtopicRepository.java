package api.assignment.backend.repository;

import api.assignment.backend.entity.Subtopic;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubtopicRepository extends JpaRepository<Subtopic, String> {
}
