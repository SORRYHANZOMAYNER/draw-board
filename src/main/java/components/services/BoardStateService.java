package components.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import components.entities.DrawingEventEntity;
import components.model.DrawingEvent;
import components.repositories.DrawingEventRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Service
public class BoardStateService {

    private final DrawingEventRepository drawingEventRepository;
    private final ObjectMapper objectMapper;

    public BoardStateService(DrawingEventRepository drawingEventRepository, ObjectMapper objectMapper) {
        this.drawingEventRepository = drawingEventRepository;
        this.objectMapper = objectMapper;
    }

    public void addEvent(Long roomId, DrawingEvent event) {
        event.setRoomId(roomId);
        DrawingEvent copy = copyEvent(event);

        String payload = objectMapper.writeValueAsString(copy);
        drawingEventRepository.save(new DrawingEventEntity(roomId, payload));
    }

    public List<DrawingEvent> getSnapshot(Long roomId) {
        List<DrawingEventEntity> stored = drawingEventRepository.findByRoomIdOrderByIdAsc(roomId);
        List<DrawingEvent> result = new ArrayList<>(stored.size());

        for (DrawingEventEntity entity : stored) {
            result.add(objectMapper.readValue(entity.getPayload(), DrawingEvent.class));
        }

        return result;
    }

    private DrawingEvent copyEvent(DrawingEvent source) {
        DrawingEvent copy = new DrawingEvent();
        copy.setType(source.getType());
        copy.setRoomId(source.getRoomId());
        copy.setStrokeId(source.getStrokeId());
        copy.setX(source.getX());
        copy.setY(source.getY());
        copy.setColor(source.getColor());
        copy.setWidth(source.getWidth());
        copy.setImageId(source.getImageId());
        copy.setData(source.getData());
        copy.setImageWidth(source.getImageWidth());
        copy.setImageHeight(source.getImageHeight());
        copy.setHeight(source.getHeight());
        copy.setStickerId(source.getStickerId());
        copy.setText(source.getText());
        copy.setShapeId(source.getShapeId());
        copy.setShapeType(source.getShapeType());
        copy.setStrokeWidth(source.getStrokeWidth());
        copy.setTextId(source.getTextId());
        copy.setFontSize(source.getFontSize());
        copy.setLocked(source.getLocked());
        return copy;
    }
}
