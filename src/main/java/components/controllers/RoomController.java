package components.controllers;

import components.entities.Room;
import components.model.DrawingEvent;
import components.repositories.RoomRepository;
import components.services.BoardStateService;
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
    @Autowired
    public RoomController(RoomService roomService, BoardStateService boardStateService) {
        this.roomService = roomService;
        this.boardStateService = boardStateService;
    }
    @PostMapping
    public ResponseEntity<Room> create(@RequestBody Room room) {
        Room roomCR = roomService.create(room);
        return new ResponseEntity<>(roomCR, HttpStatus.CREATED);
    }
    @GetMapping("/{id}")
    public ResponseEntity<Room> findById(@PathVariable Long id) {
        Room roomCR = roomService.findById(id);
        return new ResponseEntity<>(roomCR, HttpStatus.OK);
    }
    @GetMapping("/{id}/snapshot")
    public ResponseEntity<List<DrawingEvent>> getSnapshot(@PathVariable Long id) {
        return ResponseEntity.ok(boardStateService.getSnapshot(id));
    }
}
