package components.services;

import components.dto.InsertTemplateRequest;
import components.dto.InsertTemplateResponse;
import components.entities.BoardTemplate;
import components.entities.Userat;
import components.model.DrawingEvent;
import components.model.TemplateBounds;
import components.model.TemplateContent;
import components.model.TemplateEventTypes;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TemplateInsertService {

    private final TemplateService templateService;
    private final RoomService roomService;
    private final BoardStateService boardStateService;
    private final SimpMessagingTemplate messagingTemplate;

    public TemplateInsertService(
            TemplateService templateService,
            RoomService roomService,
            BoardStateService boardStateService,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.templateService = templateService;
        this.roomService = roomService;
        this.boardStateService = boardStateService;
        this.messagingTemplate = messagingTemplate;
    }

    public InsertTemplateResponse insert(
            Userat teacher,
            Long roomId,
            Long templateId,
            InsertTemplateRequest request
    ) {
        if (request.getX() == null || request.getY() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Укажите координаты вставки (x, y)");
        }

        double scale = request.getScale() != null ? request.getScale() : 1.0;
        if (scale <= 0 || scale > 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Некорректный масштаб");
        }

        roomService.getRoom(teacher, roomId);

        BoardTemplate template = templateService.requireOwnedTemplate(teacher, templateId);
        TemplateContent content = templateService.parseContent(template);
        if (content.getEvents() == null || content.getEvents().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Шаблон пуст");
        }

        String instanceId = UUID.randomUUID().toString();
        double anchorX = request.getX();
        double anchorY = request.getY();

        List<DrawingEvent> toPublish = new ArrayList<>();

        DrawingEvent groupCreate = new DrawingEvent();
        groupCreate.setType(TemplateEventTypes.GROUP_CREATE);
        groupCreate.setTemplateInstanceId(instanceId);
        groupCreate.setSavedTemplateId(template.getId());
        groupCreate.setX(anchorX);
        groupCreate.setY(anchorY);
        TemplateBounds bounds = content.getBounds();
        if (bounds != null) {
            groupCreate.setWidth(bounds.getWidth() != null ? bounds.getWidth() * scale : null);
            groupCreate.setHeight(bounds.getHeight() != null ? bounds.getHeight() * scale : null);
        }
        toPublish.add(groupCreate);

        Map<String, String> idMap = new HashMap<>();
        for (DrawingEvent source : content.getEvents()) {
            toPublish.add(transformEvent(source, roomId, instanceId, template.getId(), anchorX, anchorY, scale, idMap));
        }

        for (DrawingEvent event : toPublish) {
            boardStateService.addEvent(roomId, event);
            messagingTemplate.convertAndSend("/topic/room/" + roomId, event);
        }

        return new InsertTemplateResponse(instanceId, toPublish.size());
    }

    private DrawingEvent transformEvent(
            DrawingEvent source,
            Long roomId,
            String instanceId,
            Long savedTemplateId,
            double anchorX,
            double anchorY,
            double scale,
            Map<String, String> idMap
    ) {
        DrawingEvent event = new DrawingEvent();
        event.setType(source.getType());
        event.setRoomId(roomId);
        event.setTemplateInstanceId(instanceId);
        event.setSavedTemplateId(savedTemplateId);
        event.setColor(source.getColor());
        event.setText(source.getText());
        event.setShapeType(source.getShapeType());
        event.setData(source.getData());
        event.setLocked(source.getLocked());

        event.setStrokeId(renewId(source.getStrokeId(), idMap));
        event.setStickerId(renewId(source.getStickerId(), idMap));
        event.setImageId(renewId(source.getImageId(), idMap));
        event.setShapeId(renewId(source.getShapeId(), idMap));
        event.setTextId(renewId(source.getTextId(), idMap));

        event.setX(scaleCoord(source.getX(), scale, anchorX));
        event.setY(scaleCoord(source.getY(), scale, anchorY));
        event.setWidth(scaleSize(source.getWidth(), scale));
        event.setHeight(scaleSize(source.getHeight(), scale));
        event.setImageWidth(scaleSize(source.getImageWidth(), scale));
        event.setImageHeight(scaleSize(source.getImageHeight(), scale));
        event.setStrokeWidth(scaleSize(source.getStrokeWidth(), scale));
        event.setFontSize(scaleSize(source.getFontSize(), scale));

        return event;
    }

    private static String renewId(String id, Map<String, String> idMap) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return idMap.computeIfAbsent(id, ignored -> UUID.randomUUID().toString());
    }

    private static Double scaleCoord(Double value, double scale, double anchor) {
        if (value == null) {
            return null;
        }
        return anchor + value * scale;
    }

    private static Double scaleSize(Double value, double scale) {
        if (value == null) {
            return null;
        }
        return value * scale;
    }
}
