package components.repositories;

import components.entities.BoardTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BoardTemplateRepository extends JpaRepository<BoardTemplate, Long> {

    long countByTeacherId(Long teacherId);

    List<BoardTemplate> findByTeacherIdOrderByCreatedAtDesc(Long teacherId);

    Optional<BoardTemplate> findByIdAndTeacherId(Long id, Long teacherId);
}
