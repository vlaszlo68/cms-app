package hu.laci.cms.backend.model.settings;

import hu.laci.cms.backend.dao.common.annotations.DbColumn;
import hu.laci.cms.backend.dao.common.annotations.DbTable;
import hu.laci.cms.backend.model.common.AuditableEntity;

/**
 * Stores the single global website configuration record.
 */
@DbTable("site_settings")
public class SiteSettings extends AuditableEntity {

    @DbColumn("site_name")
    private String siteName;

    @DbColumn("logo_media_id")
    private Long logoMediaId;

    @DbColumn("footer_text")
    private String footerText;

    @DbColumn("contact_email")
    private String contactEmail;

    @DbColumn("phone")
    private String phone;

    @DbColumn("facebook_url")
    private String facebookUrl;

    @DbColumn("linkedin_url")
    private String linkedinUrl;

    public SiteSettings() {
    }

    public SiteSettings(Long id, String siteName, Long logoMediaId, String footerText, String contactEmail,
                        String phone, String facebookUrl, String linkedinUrl) {
        setId(id);
        this.siteName = siteName;
        this.logoMediaId = logoMediaId;
        this.footerText = footerText;
        this.contactEmail = contactEmail;
        this.phone = phone;
        this.facebookUrl = facebookUrl;
        this.linkedinUrl = linkedinUrl;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public Long getLogoMediaId() {
        return logoMediaId;
    }

    public void setLogoMediaId(Long logoMediaId) {
        this.logoMediaId = logoMediaId;
    }

    public String getFooterText() {
        return footerText;
    }

    public void setFooterText(String footerText) {
        this.footerText = footerText;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getFacebookUrl() {
        return facebookUrl;
    }

    public void setFacebookUrl(String facebookUrl) {
        this.facebookUrl = facebookUrl;
    }

    public String getLinkedinUrl() {
        return linkedinUrl;
    }

    public void setLinkedinUrl(String linkedinUrl) {
        this.linkedinUrl = linkedinUrl;
    }
}
