package hu.laci.cms.backend.service.media;

import java.util.List;

public class MediaServiceException extends RuntimeException {

    private final String code;
    private final List<String> validationErrors;

    public MediaServiceException(String code, String message) {
        this(code, message, null, null);
    }

    public MediaServiceException(String code, String message, Throwable cause) {
        this(code, message, cause, null);
    }

    public MediaServiceException(String code, String message, Throwable cause, List<String> validationErrors) {
        super(message, cause);
        this.code = code;
        this.validationErrors = validationErrors == null ? List.of() : List.copyOf(validationErrors);
    }

    public String getCode() {
        return code;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }
}
