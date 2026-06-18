package hu.laci.cms.backend.dto.template;

/**
 * API response describing a frontend template configuration.
 */
public class TemplateResponse {

    private final Long id;
    private final String code;
    private final String name;
    private final String description;
    private final Long previewImageMediaId;
    private final boolean active;

    public TemplateResponse(Long id, String code, String name, String description, Long previewImageMediaId,
                            boolean active) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.previewImageMediaId = previewImageMediaId;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Long getPreviewImageMediaId() {
        return previewImageMediaId;
    }

    public boolean isActive() {
        return active;
    }
}
