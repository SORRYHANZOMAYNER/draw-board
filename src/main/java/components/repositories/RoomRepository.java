package components.repositories;
import components.entities.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByOwnerIdOrderByCreated_atDesc(Long ownerId);
}
