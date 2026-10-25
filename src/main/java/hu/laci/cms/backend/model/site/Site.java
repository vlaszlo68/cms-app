package hu.laci.cms.backend.model.site;

import hu.laci.cms.backend.dao.common.annotations.DbColumn;
import hu.laci.cms.backend.dao.common.annotations.DbTable;
import hu.laci.cms.backend.model.common.AuditableEntity;

/**
 * Top-level ownership boundary for independently managed CMS content.
 */
@DbTable("sites")
public class Site extends AuditableEntity {

    @DbColumn("code")
    private String code;

    @DbColumn("name")
    private String name;

    @DbColumn("active")
    private boolean active = true;

    /** Creates an empty Site for persistence mapping. */
    public Site() {
    }

    /**
     * Creates a Site with its editable fields.
     *
     * @param id persisted id, or {@code null} for a new Site
     * @param code stable machine-readable code
     * @param name administrator-facing name
     * @param active whether public APIs may serve the Site
     */
    public Site(Long id, String code, String name, boolean active) {
        setId(id);
        this.code = code;
        this.name = name;
        this.active = active;
    }

    /** @return stable machine-readable code */
    public String getCode() {
        return code;
    }

    /** @param code stable machine-readable code */
    public void setCode(String code) {
        this.code = code;
    }

    /** @return administrator-facing Site name */
    public String getName() {
        return name;
    }

    /** @param name administrator-facing Site name */
    public void setName(String name) {
        this.name = name;
    }

    /** @return whether public APIs may serve the Site */
    public boolean isActive() {
        return active;
    }

    /** @param active whether public APIs may serve the Site */
    public void setActive(boolean active) {
        this.active = active;
    }
}
