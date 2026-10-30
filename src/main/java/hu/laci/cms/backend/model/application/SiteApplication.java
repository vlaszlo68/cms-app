package hu.laci.cms.backend.model.application;

import hu.laci.cms.backend.dao.common.annotations.DbColumn;
import hu.laci.cms.backend.dao.common.annotations.DbTable;
import hu.laci.cms.backend.model.common.AuditableEntity;
import hu.laci.cms.backend.model.site.Site;

/** Site-specific enablement and display configuration for a global application definition. */
@DbTable("site_applications")
public class SiteApplication extends AuditableEntity {
    @DbColumn("site_id") private Site site;
    @DbColumn("application_code") private String applicationCode;
    @DbColumn("name") private String name;
    @DbColumn("active") private boolean active = true;
    public SiteApplication() { }
    public SiteApplication(Long id, Site site, String applicationCode, String name, boolean active) {
        setId(id); this.site = site; this.applicationCode = applicationCode; this.name = name; this.active = active;
    }
    public Site getSite() { return site; }
    public void setSite(Site site) { this.site = site; }
    public String getApplicationCode() { return applicationCode; }
    public void setApplicationCode(String applicationCode) { this.applicationCode = applicationCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
