package hu.laci.cms.backend.service.template;

/**
 * Business exception produced by template operations.
 */
public class TemplateServiceException extends RuntimeException {

    private final String code;

    public TemplateServiceException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
