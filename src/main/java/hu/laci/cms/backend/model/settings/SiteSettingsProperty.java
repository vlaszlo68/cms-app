package hu.laci.cms.backend.model.settings;

import hu.laci.cms.backend.model.common.AuditableProperty;
import hu.laci.cms.backend.model.common.BaseProperty;

/**
 * Query properties available for the singleton {@link SiteSettings} entity.
 */
public final class SiteSettingsProperty extends AuditableProperty {

    public static final SiteSettingsProperty ID = new SiteSettingsProperty(BaseProperty.ID.getPropertyName());
    public static final SiteSettingsProperty SITE_NAME = new SiteSettingsProperty("siteName");
    public static final SiteSettingsProperty LOGO_MEDIA_ID = new SiteSettingsProperty("logoMediaId");
    public static final SiteSettingsProperty FOOTER_TEXT = new SiteSettingsProperty("footerText");
    public static final SiteSettingsProperty CONTACT_EMAIL = new SiteSettingsProperty("contactEmail");
    public static final SiteSettingsProperty PHONE = new SiteSettingsProperty("phone");
    public static final SiteSettingsProperty FACEBOOK_URL = new SiteSettingsProperty("facebookUrl");
    public static final SiteSettingsProperty LINKEDIN_URL = new SiteSettingsProperty("linkedinUrl");
    public static final SiteSettingsProperty CREATED_AT = new SiteSettingsProperty(AuditableProperty.CREATED_AT.getPropertyName());
    public static final SiteSettingsProperty UPDATED_AT = new SiteSettingsProperty(AuditableProperty.UPDATED_AT.getPropertyName());
    public static final SiteSettingsProperty CREATED_BY = new SiteSettingsProperty(AuditableProperty.CREATED_BY.getPropertyName());
    public static final SiteSettingsProperty UPDATED_BY = new SiteSettingsProperty(AuditableProperty.UPDATED_BY.getPropertyName());

    private SiteSettingsProperty(String propertyName) {
        super(SiteSettings.class, propertyName);
    }
}
