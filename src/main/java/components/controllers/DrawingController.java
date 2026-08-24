package components.controllers;

import components.model.DrawingEvent;
import components.services.BoardStateService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class DrawingController {

    private final SimpMessagingTemplate messagingTemplate;
    private final BoardStateService boardStateService;

    public DrawingController(
            SimpMessagingTemplate messagingTemplate,
            BoardStateService boardStateService
    ) {
        this.messagingTemplate = messagingTemplate;
        this.boardStateService = boardStateService;
    }

    @MessageMapping("/room/{roomId}/draw")
    public void handleDraw(
            @DestinationVariable Long roomId,
            DrawingEvent event
    ) {
        event.setRoomId(roomId);
        boardStateService.addEvent(roomId, event);
        messagingTemplate.convertAndSend("/topic/room/" + roomId, event);
    }
}