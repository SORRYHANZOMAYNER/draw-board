package components.services;

import components.model.DrawingEvent;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class BoardStateService {

    private final Map<Long, List<DrawingEvent>> roomEvents = new ConcurrentHashMap<>();

    public void addEvent(Long roomId, DrawingEvent event) {
        roomEvents
                .computeIfAbsent(roomId, id -> new CopyOnWriteArrayList<>())
                .add(copyEvent(event));
    }

    public List<DrawingEvent> getSnapshot(Long roomId) {
        return new ArrayList<>(roomEvents.getOrDefault(roomId, List.of()));
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
        return copy;
    }
}