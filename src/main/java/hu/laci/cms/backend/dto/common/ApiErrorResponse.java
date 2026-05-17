package hu.laci.cms.backend.dto.common;

/**
 * Error payload used inside the common {@link ApiResponse} envelope.
 */
public class ApiErrorResponse {

    private final String code;
    private final String message;

    /**
     * Creates an API error payload.
     *
     * @param code stable machine-readable error code
     * @param message human-readable error message
     */
    public ApiErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * Returns the stable error code.
     *
     * @return error code
     */
    public String getCode() {
        return code;
    }

    /**
     * Returns the human-readable error message.
     *
     * @return error message
     */
    public String getMessage() {
        return message;
    }
}
