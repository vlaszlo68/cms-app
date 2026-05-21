package hu.laci.cms.backend.service.user;

/**
 * Business exception thrown by user management operations.
 */
public class UserServiceException extends RuntimeException {

    private final String code;

    /**
     * Creates a user service exception with an API-stable code.
     *
     * @param code stable error code
     * @param message human-readable message
     */
    public UserServiceException(String code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * Returns the stable error code.
     *
     * @return error code
     */
    public String getCode() {
        return code;
    }
}
