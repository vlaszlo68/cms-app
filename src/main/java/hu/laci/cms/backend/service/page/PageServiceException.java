package hu.laci.cms.backend.service.page;

import java.util.List;

/**
 * Business exception thrown by CMS page operations.
 */
public class PageServiceException extends RuntimeException {

    private final String code;
    private final List<String> validationErrors;

    public PageServiceException(String code, String message) {
        this(code, message, null);
    }

    public PageServiceException(String code, String message, List<String> validationErrors) {
        super(message);
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
