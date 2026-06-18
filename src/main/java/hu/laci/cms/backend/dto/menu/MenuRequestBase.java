package hu.laci.cms.backend.dto.menu;

public abstract class MenuRequestBase {

    private String name;
    private String code;
    private Boolean active;

    protected MenuRequestBase() {
    }

    protected MenuRequestBase(String name, String code, Boolean active) {
        this.name = name;
        this.code = code;
        this.active = active;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public Boolean getActive() {
        return active;
    }
}
