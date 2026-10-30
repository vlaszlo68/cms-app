package hu.laci.cms.backend.service.application;
/** Controlled business exception for SiteApplication administration and public resolution. */
public class SiteApplicationServiceException extends RuntimeException {

    private final String code;

    /**
     * Creates an error with a stable API error code.
     *
     * @param code stable application error code
     * @param message safe client-facing message
     */
    public SiteApplicationServiceException(String code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * Returns the stable error code exposed by the servlet layer.
     *
     * @return error code
     */
    public String getCode() {
        return code;
    }
}
