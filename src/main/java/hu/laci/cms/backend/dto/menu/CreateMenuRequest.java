package hu.laci.cms.backend.dto.menu;

public class CreateMenuRequest extends MenuRequestBase {

    public CreateMenuRequest() {
    }

    public CreateMenuRequest(String name, String code, Boolean active) {
        super(name, code, active);
    }
}
