package components.dto;

public class InsertTemplateResponse {

    private String templateInstanceId;
    private int eventCount;

    public InsertTemplateResponse(String templateInstanceId, int eventCount) {
        this.templateInstanceId = templateInstanceId;
        this.eventCount = eventCount;
    }

    public String getTemplateInstanceId() {
        return templateInstanceId;
    }

    public int getEventCount() {
        return eventCount;
    }
}
