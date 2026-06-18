package hu.laci.cms.backend.dto.settings;

/**
 * API response containing global website configuration.
 */
public class SiteSettingsResponse {

    private final Long id;
    private final String siteName;
    private final Long logoMediaId;
    private final String footerText;
    private final String contactEmail;
    private final String phone;
    private final String facebookUrl;
    private final String linkedinUrl;

    public SiteSettingsResponse(Long id, String siteName, Long logoMediaId, String footerText, String contactEmail,
                                String phone, String facebookUrl, String linkedinUrl) {
        this.id = id;
        this.siteName = siteName;
        this.logoMediaId = logoMediaId;
        this.footerText = footerText;
        this.contactEmail = contactEmail;
        this.phone = phone;
        this.facebookUrl = facebookUrl;
        this.linkedinUrl = linkedinUrl;
    }

    public Long getId() {
        return id;
    }

    public String getSiteName() {
        return siteName;
    }

    public Long getLogoMediaId() {
        return logoMediaId;
    }

    public String getFooterText() {
        return footerText;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public String getPhone() {
        return phone;
    }

    public String getFacebookUrl() {
        return facebookUrl;
    }

    public String getLinkedinUrl() {
        return linkedinUrl;
    }
}
