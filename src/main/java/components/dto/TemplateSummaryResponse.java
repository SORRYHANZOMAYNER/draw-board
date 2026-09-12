package components.dto;

import components.model.TemplateBounds;

import java.util.Date;

public class TemplateSummaryResponse {

    private Long id;
    private String name;
    private TemplateBounds bounds;
    private Date createdAt;
    private Date updatedAt;
    private boolean hasPreview;

    public TemplateSummaryResponse(
            Long id,
            String name,
            TemplateBounds bounds,
            Date createdAt,
            Date updatedAt,
            boolean hasPreview
    ) {
        this.id = id;
        this.name = name;
        this.bounds = bounds;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.hasPreview = hasPreview;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public TemplateBounds getBounds() {
        return bounds;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public boolean isHasPreview() {
        return hasPreview;
    }
}
