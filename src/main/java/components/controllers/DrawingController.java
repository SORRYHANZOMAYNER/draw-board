package components.controllers;

import components.model.DrawingEvent;
import components.services.BoardStateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class DrawingController {

    private static final Logger log = LoggerFactory.getLogger(DrawingController.class);

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
        try {
            boardStateService.addEvent(roomId, event);
            messagingTemplate.convertAndSend("/topic/room/" + roomId, event);
        } catch (RuntimeException ex) {
            log.error("Failed to persist or broadcast {} for room {}", event.getType(), roomId, ex);
            throw ex;
        }
    }
}