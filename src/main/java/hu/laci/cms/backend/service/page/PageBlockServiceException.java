package hu.laci.cms.backend.service.page;

/**
 * Business exception produced by page block operations.
 */
public class PageBlockServiceException extends RuntimeException {

    private final String code;

    public PageBlockServiceException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
