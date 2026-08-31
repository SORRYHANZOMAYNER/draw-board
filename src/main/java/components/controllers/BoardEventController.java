package components.controllers;

import components.config.SecurityUtils;
import components.entities.Room;
import components.entities.Userat;
import components.model.DrawingEvent;
import components.services.BoardStateService;
import components.services.RoomAccessService;
import components.services.RoomService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/room/{roomId}/events")
public class BoardEventController {

    private final RoomService roomService;
    private final BoardStateService boardStateService;
    private final RoomAccessService roomAccessService;
    private final SimpMessagingTemplate messagingTemplate;

    public BoardEventController(
            RoomService roomService,
            BoardStateService boardStateService,
            RoomAccessService roomAccessService,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.roomService = roomService;
        this.boardStateService = boardStateService;
        this.roomAccessService = roomAccessService;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping
    public ResponseEntity<Void> addEvent(
            @PathVariable Long roomId,
            @RequestBody DrawingEvent event
    ) {
        Userat user = SecurityUtils.getCurrentUser();
        Room room = roomService.getRoom(user, roomId);
        if (room == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found");
        }

        roomAccessService.checkAccess(SecurityUtils.getCurrentUser(), room);

        event.setRoomId(roomId);
        boardStateService.addEvent(roomId, event);
        messagingTemplate.convertAndSend("/topic/room/" + roomId, event);

        return ResponseEntity.noContent().build();
    }
}
