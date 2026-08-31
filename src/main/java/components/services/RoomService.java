package components.services;
import components.dto.CreateRoomRequest;
import components.entities.Room;
import components.entities.Userat;
import components.repositories.DrawingEventRepository;
import components.repositories.RoomRepository;
import components.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.Date;
import java.util.List;
@Service
public class RoomService {
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final RoomAccessService roomAccessService;
    private final DrawingEventRepository drawingEventRepository;

    @Autowired
    public RoomService(RoomRepository roomRepository, UserRepository userRepository, RoomAccessService roomAccessService, DrawingEventRepository drawingEventRepository) {
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.roomAccessService = roomAccessService;
        this.drawingEventRepository = drawingEventRepository;
    }

    public Room createOwnRoom(Userat student, CreateRoomRequest request) {
        validateRoomName(request);
        Room room = new Room();
        room.setName(request.getName().trim());
        room.setCreatedAt(new Date());
        room.setOwnerId(student.getId());
        return roomRepository.save(room);
    }

    public Room renameRoom(Userat user, Long id, String name) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room name is required");
        }

        Room room = getRoom(user, id);
        room.setName(name.trim());
        return roomRepository.save(room);
    }

    public void deleteRoom(Userat user, Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room id is required");
        }

        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        roomAccessService.requireOwner(user, room);

        drawingEventRepository.findByRoomIdOrderByIdAsc(id)
                .forEach(drawingEventRepository::delete);
        roomRepository.delete(room);
    }

    public Room createRoomForStudent(Userat teacher, Long studentId, CreateRoomRequest request) {
        validateRoomName(request);
        Userat student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        if (student.getRole() != components.entities.Role.STUDENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target user is not a student");
        }
        Room room = new Room();
        room.setName(request.getName().trim());
        room.setCreatedAt(new Date());
        room.setOwnerId(student.getId());
        return roomRepository.save(room);
    }

    public List<Room> findMine(Userat user) {
        return roomRepository.findByOwnerIdOrderByCreatedAtDesc(user.getId());
    }

    public List<Room> findByStudent(Userat teacher, Long studentId) {
        Userat student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        if (student.getRole() != components.entities.Role.STUDENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target user is not a student");
        }
        return roomRepository.findByOwnerIdOrderByCreatedAtDesc(studentId);
    }

    public Room getRoom(Userat user, Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        roomAccessService.checkAccess(user, room);
        return room;
    }

    private void validateRoomName(CreateRoomRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room name is required");
        }
    }
}