package hu.laci.cms.backend.service.site;

/**
 * Controlled business exception for Site management and resolution.
 */
public class SiteServiceException extends RuntimeException {

    private final String code;

    /**
     * Creates a controlled Site service exception.
     *
     * @param code stable API error code
     * @param message safe client-facing message
     */
    public SiteServiceException(String code, String message) {
        super(message);
        this.code = code;
    }

    /** @return stable API error code */
    public String getCode() {
        return code;
    }
}
