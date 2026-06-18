package hu.laci.cms.backend.model.template;

import hu.laci.cms.backend.dao.common.annotations.DbColumn;
import hu.laci.cms.backend.dao.common.annotations.DbTable;
import hu.laci.cms.backend.model.common.AuditableEntity;

/**
 * Configures a frontend-rendered page layout without storing executable template code.
 */
@DbTable("templates")
public class Template extends AuditableEntity {

    @DbColumn("code")
    private String code;

    @DbColumn("name")
    private String name;

    @DbColumn("description")
    private String description;

    @DbColumn("preview_image_media_id")
    private Long previewImageMediaId;

    @DbColumn("active")
    private boolean active = true;

    public Template() {
    }

    public Template(Long id, String code, String name, String description, Long previewImageMediaId,
                    boolean active) {
        setId(id);
        this.code = code;
        this.name = name;
        this.description = description;
        this.previewImageMediaId = previewImageMediaId;
        this.active = active;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getPreviewImageMediaId() {
        return previewImageMediaId;
    }

    public void setPreviewImageMediaId(Long previewImageMediaId) {
        this.previewImageMediaId = previewImageMediaId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
