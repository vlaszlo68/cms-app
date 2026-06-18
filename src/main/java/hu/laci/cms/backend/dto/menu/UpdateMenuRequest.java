package hu.laci.cms.backend.dto.menu;

public class UpdateMenuRequest extends MenuRequestBase {

    public UpdateMenuRequest() {
    }

    public UpdateMenuRequest(String name, String code, Boolean active) {
        super(name, code, active);
    }
}
