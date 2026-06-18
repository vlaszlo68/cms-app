package hu.laci.cms.backend.service.menu;

public class MenuServiceException extends RuntimeException {

    private final String code;

    public MenuServiceException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
