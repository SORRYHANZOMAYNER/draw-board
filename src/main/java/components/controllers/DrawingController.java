package components.controllers;

import components.model.DrawingEvent;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class DrawingController {

    private final SimpMessagingTemplate messagingTemplate;

    public DrawingController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/room/{roomId}/draw")
    public void handleDraw(
            @DestinationVariable Long roomId,
            DrawingEvent event
    ) {
        event.setRoomId(roomId);
        messagingTemplate.convertAndSend("/topic/room/" + roomId, event);
    }
}