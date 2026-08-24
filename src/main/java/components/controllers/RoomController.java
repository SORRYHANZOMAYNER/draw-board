package components.controllers;

import components.entities.Room;
import components.repositories.RoomRepository;
import components.services.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/room")
public class RoomController {
    private final RoomService roomService;
    @Autowired
    public RoomController(RoomService roomService) {
        this.roomService = roomService;
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
}
