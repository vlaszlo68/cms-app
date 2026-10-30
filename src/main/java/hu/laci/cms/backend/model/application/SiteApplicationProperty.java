package hu.laci.cms.backend.model.application;
import hu.laci.cms.backend.model.common.AuditableProperty;
import hu.laci.cms.backend.model.common.BaseProperty;
/** Query properties for SiteApplication. */
public final class SiteApplicationProperty extends AuditableProperty {
    public static final SiteApplicationProperty ID = new SiteApplicationProperty(BaseProperty.ID.getPropertyName());
    public static final SiteApplicationProperty SITE = new SiteApplicationProperty("site");
    public static final SiteApplicationProperty APPLICATION_CODE = new SiteApplicationProperty("applicationCode");
    public static final SiteApplicationProperty ACTIVE = new SiteApplicationProperty("active");
    private SiteApplicationProperty(String name) { super(SiteApplication.class, name); }
}
