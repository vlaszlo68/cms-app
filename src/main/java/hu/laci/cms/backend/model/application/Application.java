package hu.laci.cms.backend.model.application;

import hu.laci.cms.backend.dao.common.annotations.DbColumn;
import hu.laci.cms.backend.dao.common.annotations.DbTable;
import hu.laci.cms.backend.model.common.AuditableEntity;
import hu.laci.cms.backend.model.site.Site;

/** Site-owned configuration for one explicitly supported public application type. */
@DbTable("applications")
public class Application extends AuditableEntity {
    @DbColumn("site_id") private Site site;
    @DbColumn("code") private String code;
    @DbColumn("name") private String name;
    @DbColumn("active") private boolean active = true;
    public Application() { }
    public Application(Long id, Site site, String code, String name, boolean active) {
        setId(id); this.site = site; this.code = code; this.name = name; this.active = active;
    }
    public Site getSite() { return site; }
    public void setSite(Site site) { this.site = site; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
