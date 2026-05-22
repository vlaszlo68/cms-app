package hu.laci.cms.backend.service.user;

import java.util.List;

/**
 * Business exception thrown by user management operations.
 */
public class UserServiceException extends RuntimeException {

    private final String code;
    private final List<String> validationErrors;

    /**
     * Creates a user service exception with an API-stable code.
     *
     * @param code stable error code
     * @param message human-readable message
     */
    public UserServiceException(String code, String message) {
        this(code, message, null);
    }

    /**
     * Creates a user service exception with structured validation error codes.
     *
     * @param code stable error code
     * @param message human-readable message
     * @param validationErrors stable validation error codes, or null when not applicable
     */
    public UserServiceException(String code, String message, List<String> validationErrors) {
        super(message);
        this.code = code;
        this.validationErrors = validationErrors == null ? List.of() : List.copyOf(validationErrors);
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
     * Returns structured validation error codes.
     *
     * @return validation error codes, empty when not applicable
     */
    public List<String> getValidationErrors() {
        return validationErrors;
    }
}
