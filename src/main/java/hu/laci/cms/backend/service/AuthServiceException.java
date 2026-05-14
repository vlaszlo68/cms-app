package hu.laci.cms.backend.service;

public class AuthServiceException extends RuntimeException {

    public AuthServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
