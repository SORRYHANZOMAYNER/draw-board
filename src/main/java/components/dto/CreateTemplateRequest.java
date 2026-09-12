package components.dto;

import components.model.DrawingEvent;
import components.model.TemplateBounds;

import java.util.List;

public class CreateTemplateRequest {

    private String name;
    private TemplateBounds bounds;
    private List<DrawingEvent> events;
    private String previewData;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TemplateBounds getBounds() {
        return bounds;
    }

    public void setBounds(TemplateBounds bounds) {
        this.bounds = bounds;
    }

    public List<DrawingEvent> getEvents() {
        return events;
    }

    public void setEvents(List<DrawingEvent> events) {
        this.events = events;
    }

    public String getPreviewData() {
        return previewData;
    }

    public void setPreviewData(String previewData) {
        this.previewData = previewData;
    }
}
