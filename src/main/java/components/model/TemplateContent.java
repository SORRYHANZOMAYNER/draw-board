package components.model;

import java.util.List;

public class TemplateContent {

    public static final int CURRENT_VERSION = 1;

    private int version = CURRENT_VERSION;
    private TemplateBounds bounds;
    private List<DrawingEvent> events;

    public TemplateContent() {
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
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
}
