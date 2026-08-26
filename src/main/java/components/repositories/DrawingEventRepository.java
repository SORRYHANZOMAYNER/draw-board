package components.repositories;
import components.entities.DrawingEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface DrawingEventRepository extends JpaRepository<DrawingEventEntity, Long> {
    List<DrawingEventEntity> findByRoomIdOrderByIdAsc(Long roomId);
}