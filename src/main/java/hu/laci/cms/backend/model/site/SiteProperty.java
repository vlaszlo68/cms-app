package hu.laci.cms.backend.model.site;

import hu.laci.cms.backend.model.common.AuditableProperty;
import hu.laci.cms.backend.model.common.BaseProperty;

/**
 * Query properties for {@link Site} persistence operations.
 */
public final class SiteProperty extends AuditableProperty {

    public static final SiteProperty ID = new SiteProperty(BaseProperty.ID.getPropertyName());
    public static final SiteProperty CODE = new SiteProperty("code");
    public static final SiteProperty NAME = new SiteProperty("name");
    public static final SiteProperty ACTIVE = new SiteProperty("active");
    public static final SiteProperty THEME_CODE = new SiteProperty("themeCode");
    public static final SiteProperty CREATED_AT = new SiteProperty(AuditableProperty.CREATED_AT.getPropertyName());
    public static final SiteProperty UPDATED_AT = new SiteProperty(AuditableProperty.UPDATED_AT.getPropertyName());
    public static final SiteProperty CREATED_BY = new SiteProperty(AuditableProperty.CREATED_BY.getPropertyName());
    public static final SiteProperty UPDATED_BY = new SiteProperty(AuditableProperty.UPDATED_BY.getPropertyName());

    private SiteProperty(String propertyName) {
        super(Site.class, propertyName);
    }
}
