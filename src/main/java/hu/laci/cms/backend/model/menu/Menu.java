package hu.laci.cms.backend.model.menu;

import hu.laci.cms.backend.dao.common.annotations.DbColumn;
import hu.laci.cms.backend.dao.common.annotations.DbTable;
import hu.laci.cms.backend.model.common.AuditableEntity;
import hu.laci.cms.backend.model.site.Site;

/**
 * Site-owned navigation menu identified by a code unique within its Site.
 */
@DbTable("menus")
public class Menu extends AuditableEntity {

    @DbColumn("site_id")
    private Site site;

    @DbColumn("name")
    private String name;

    @DbColumn("code")
    private String code;

    @DbColumn("active")
    private boolean active = true;

    public Menu() {
    }

    public Menu(Long id, String name, String code, boolean active) {
        setId(id);
        this.name = name;
        this.code = code;
        this.active = active;
    }

    /** @return Site that owns this menu */
    /** @return Site that owns this menu */
    public Site getSite() {
        return site;
    }

    /** @param site Site that owns this menu */
    /** @param site Site that owns this menu */
    public void setSite(Site site) {
        this.site = site;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
