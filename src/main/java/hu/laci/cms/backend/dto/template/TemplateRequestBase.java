package hu.laci.cms.backend.dto.template;

/**
 * Shared request fields for template creation and update.
 */
public abstract class TemplateRequestBase {

    private String code;
    private String name;
    private String description;
    private Long previewImageMediaId;
    private Boolean active;

    protected TemplateRequestBase() {
    }

    protected TemplateRequestBase(String code, String name, String description, Long previewImageMediaId,
                                  Boolean active) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.previewImageMediaId = previewImageMediaId;
        this.active = active;
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

    public Boolean getActive() {
        return active;
    }
}
