package components.dto;

import components.model.TemplateContent;

import java.util.Date;

public class TemplateDetailResponse {

    private Long id;
    private String name;
    private TemplateContent content;
    private String previewData;
    private Date createdAt;
    private Date updatedAt;

    public TemplateDetailResponse(
            Long id,
            String name,
            TemplateContent content,
            String previewData,
            Date createdAt,
            Date updatedAt
    ) {
        this.id = id;
        this.name = name;
        this.content = content;
        this.previewData = previewData;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public TemplateContent getContent() {
        return content;
    }

    public String getPreviewData() {
        return previewData;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }
}
