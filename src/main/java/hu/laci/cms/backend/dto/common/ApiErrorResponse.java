package hu.laci.cms.backend.dto.common;

import java.util.List;

/**
 * Error payload used inside the common {@link ApiResponse} envelope.
 */
public class ApiErrorResponse {

    private final String code;
    private final String message;
    private final List<String> validationErrors;

    /**
     * Creates an API error payload.
     *
     * @param code stable machine-readable error code
     * @param message human-readable error message
     */
    public ApiErrorResponse(String code, String message) {
        this(code, message, null);
    }

    /**
     * Creates an API error payload with structured validation error codes.
     *
     * @param code stable machine-readable error code
     * @param message human-readable error message
     * @param validationErrors stable validation error codes, or null when not applicable
     */
    public ApiErrorResponse(String code, String message, List<String> validationErrors) {
        this.code = code;
        this.message = message;
        this.validationErrors = validationErrors == null ? null : List.copyOf(validationErrors);
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

    /**
     * Returns field or policy validation error codes.
     *
     * @return validation error codes, or {@code null} for non-validation errors
     */
    public List<String> getValidationErrors() {
        return validationErrors;
    }
}
