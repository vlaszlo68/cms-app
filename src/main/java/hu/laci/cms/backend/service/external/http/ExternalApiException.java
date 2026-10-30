package hu.laci.cms.backend.service.external.http;

/** Controlled outbound failure that deliberately excludes vendor response text and Java transport details. */
public final class ExternalApiException extends RuntimeException {
    public static final String TIMEOUT = "EXTERNAL_SERVICE_TIMEOUT";
    public static final String UNAVAILABLE = "EXTERNAL_SERVICE_UNAVAILABLE";
    public static final String RATE_LIMITED = "EXTERNAL_SERVICE_RATE_LIMITED";
    public static final String INVALID_RESPONSE = "EXTERNAL_SERVICE_INVALID_RESPONSE";
    public static final String AUTHENTICATION = "EXTERNAL_SERVICE_AUTHENTICATION";
    public static final String REJECTED = "EXTERNAL_SERVICE_REJECTED";
    private final String code;
    private final Integer httpStatus;

    /** Creates an external failure that occurred before an HTTP response was available. */
    public ExternalApiException(String code, String message) { this(code, message, null); }

    /** Creates an external failure with the provider status when one was received. */
    public ExternalApiException(String code, String message, Integer httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    /** Returns the stable application error code. */
    public String getCode() { return code; }

    /** Returns the provider HTTP status, or {@code null} when transport failed before a response. */
    public Integer getHttpStatus() { return httpStatus; }
}
