package api.assignment.backend.repository;

import api.assignment.backend.entity.SubtopicProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubtopicProgressRepository extends JpaRepository<SubtopicProgress, Long> {
    Optional<SubtopicProgress> findByUserIdAndSubtopicId(Long userId, String subtopicId);
    List<SubtopicProgress> findAllByUserIdAndSubtopicIdIn(Long userId, List<String> subtopicIds);
}
