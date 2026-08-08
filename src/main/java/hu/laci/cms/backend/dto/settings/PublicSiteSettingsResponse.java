package hu.laci.cms.backend.dto.settings;

/**
 * Anonymous public representation of the global site settings.
 *
 * <p>This response deliberately excludes the administrative singleton id and contains
 * only fields needed to render the public layout.</p>
 */
public class PublicSiteSettingsResponse {

    private final String siteName;
    private final Long logoMediaId;
    private final String footerText;
    private final String contactEmail;
    private final String phone;
    private final String facebookUrl;
    private final String linkedinUrl;

    /**
     * Creates the public layout settings response.
     *
     * @param siteName website name
     * @param logoMediaId optional logo media id
     * @param footerText optional footer text
     * @param contactEmail optional public contact email
     * @param phone optional public phone number
     * @param facebookUrl optional Facebook URL
     * @param linkedinUrl optional LinkedIn URL
     */
    public PublicSiteSettingsResponse(String siteName, Long logoMediaId, String footerText, String contactEmail,
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
