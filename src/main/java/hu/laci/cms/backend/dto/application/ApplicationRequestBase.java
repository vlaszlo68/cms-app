package hu.laci.cms.backend.dto.application;
/** Shared editable application fields. */
public abstract class ApplicationRequestBase {
    private String code; private String name; private Boolean active;
    protected ApplicationRequestBase() { }
    public String getCode() { return code; } public String getName() { return name; } public Boolean getActive() { return active; }
    public void setCode(String code) { this.code = code; } public void setName(String name) { this.name = name; } public void setActive(Boolean active) { this.active = active; }
}
