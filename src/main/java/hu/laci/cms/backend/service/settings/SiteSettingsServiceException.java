package hu.laci.cms.backend.service.settings;

/**
 * Business exception produced by site settings operations.
 */
public class SiteSettingsServiceException extends RuntimeException {

    private final String code;

    public SiteSettingsServiceException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
