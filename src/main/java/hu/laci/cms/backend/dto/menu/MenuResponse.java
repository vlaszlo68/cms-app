package hu.laci.cms.backend.dto.menu;

public class MenuResponse {

    private final Long id;
    private final String name;
    private final String code;
    private final boolean active;

    public MenuResponse(Long id, String name, String code, boolean active) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public boolean isActive() {
        return active;
    }
}
