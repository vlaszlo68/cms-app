package hu.laci.cms.backend.service.site;

/**
 * Controlled business exception for Site management and resolution.
 */
public class SiteServiceException extends RuntimeException {

    private final String code;
    private final Object details;

    /**
     * Creates a controlled Site service exception.
     *
     * @param code stable API error code
     * @param message safe client-facing message
     */
    public SiteServiceException(String code, String message) {
        this(code, message, null);
    }

    /**
     * Creates a controlled Site service exception with structured client-safe details.
     *
     * @param code stable API error code
     * @param message safe client-facing message
     * @param details optional structured error details
     */
    public SiteServiceException(String code, String message, Object details) {
        super(message);
        this.code = code;
        this.details = details;
    }

    /** @return stable API error code */
    public String getCode() {
        return code;
    }

    /** @return optional structured client-safe error details */
    public Object getDetails() { return details; }
}
