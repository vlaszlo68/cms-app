package hu.laci.cms.backend.dto.settings;

/**
 * Request DTO for replacing global website settings.
 */
public class SaveSiteSettingsRequest {

    private String siteName;
    private Long logoMediaId;
    private String footerText;
    private String contactEmail;
    private String phone;
    private String facebookUrl;
    private String linkedinUrl;

    public SaveSiteSettingsRequest() {
    }

    public SaveSiteSettingsRequest(String siteName, Long logoMediaId, String footerText, String contactEmail,
                                   String phone, String facebookUrl, String linkedinUrl) {
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
