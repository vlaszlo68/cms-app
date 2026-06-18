package hu.laci.cms.backend.model.menu;

import hu.laci.cms.backend.dao.common.annotations.DbColumn;
import hu.laci.cms.backend.dao.common.annotations.DbTable;
import hu.laci.cms.backend.model.common.AuditableEntity;

@DbTable("menus")
public class Menu extends AuditableEntity {

    @DbColumn("name")
    private String name;

    @DbColumn("code")
    private String code;

    @DbColumn("active")
    private boolean active = true;

    public Menu() {
    }

    public Menu(Long id, String name, String code, boolean active) {
        setId(id);
        this.name = name;
        this.code = code;
        this.active = active;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
