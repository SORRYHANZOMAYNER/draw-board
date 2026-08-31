package components.controllers;

import components.config.SecurityUtils;
import components.dto.CreateRoomRequest;
import components.dto.UpdateRoomRequest;
import components.dto.UserBriefResponse;
import components.entities.Room;
import components.entities.Userat;
import components.model.DrawingEvent;
import components.repositories.UserRepository;
import components.services.BoardStateService;
import components.services.RoomAccessService;
import components.services.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/room")
public class RoomController {

    private final RoomService roomService;
    private final BoardStateService boardStateService;
    private final UserRepository userRepository;

    @Autowired
    public RoomController(
            RoomService roomService,
            BoardStateService boardStateService,
            UserRepository userRepository
    ) {
        this.roomService = roomService;
        this.boardStateService = boardStateService;
        this.userRepository = userRepository;
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Room> renameRoom(
            @PathVariable Long id,
            @RequestBody UpdateRoomRequest request
    ) {
        Userat user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(roomService.renameRoom(user, id, request.getName()));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        Userat user = SecurityUtils.getCurrentUser();
        roomService.deleteRoom(user, id);
        return ResponseEntity.noContent().build();
    }

    /** Доски текущего ученика */
    @GetMapping("/mine")
    public ResponseEntity<List<Room>> getMyRooms() {
        Userat user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(roomService.findMine(user));
    }

    /** Ученик создаёт свою доску */
    @PostMapping
    public ResponseEntity<Room> createOwnRoom(@RequestBody CreateRoomRequest request) {
        Userat user = SecurityUtils.getCurrentUser();
        Room room = roomService.createOwnRoom(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(room);
    }

    /** Учитель ищет учеников по имени */
    @GetMapping("/students/search")
    public ResponseEntity<List<UserBriefResponse>> searchStudents(@RequestParam("q") String query) {
        Userat teacher = SecurityUtils.getCurrentUser();
        SecurityUtils.requireTeacher(teacher);

        if (query == null || query.isBlank()) {
            return ResponseEntity.ok(List.of());
        }

        List<UserBriefResponse> students = userRepository
                .findByUsernameContainingIgnoreCaseAndRole(query.trim(), components.entities.Role.STUDENT)
                .stream()
                .map(u -> new UserBriefResponse(u.getId(), u.getUsername(), u.getRole()))
                .toList();

        return ResponseEntity.ok(students);
    }

    /** Учитель смотрит доски ученика */
    @GetMapping("/students/{studentId}/rooms")
    public ResponseEntity<List<Room>> getStudentRooms(@PathVariable Long studentId) {
        Userat teacher = SecurityUtils.getCurrentUser();
        SecurityUtils.requireTeacher(teacher);
        return ResponseEntity.ok(roomService.findByStudent(teacher, studentId));
    }

    /** Учитель создаёт блокнот для ученика */
    @PostMapping("/students/{studentId}/rooms")
    public ResponseEntity<Room> createRoomForStudent(
            @PathVariable Long studentId,
            @RequestBody CreateRoomRequest request
    ) {
        Userat teacher = SecurityUtils.getCurrentUser();
        SecurityUtils.requireTeacher(teacher);
        Room room = roomService.createRoomForStudent(teacher, studentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(room);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Room> findById(@PathVariable Long id) {
        Userat user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(roomService.getRoom(user, id));
    }

    @GetMapping("/{id}/snapshot")
    public ResponseEntity<List<DrawingEvent>> getSnapshot(@PathVariable Long id) {
        Userat user = SecurityUtils.getCurrentUser();
        roomService.getRoom(user, id);
        return ResponseEntity.ok(boardStateService.getSnapshot(id));
    }
}