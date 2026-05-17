package hu.laci.cms.backend.service;

/**
 * Runtime exception for authentication service failures that are not expected
 * invalid-credential results.
 */
public class AuthServiceException extends RuntimeException {

    /**
     * Creates an authentication service exception.
     *
     * @param message error message
     * @param cause original failure
     */
    public AuthServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
