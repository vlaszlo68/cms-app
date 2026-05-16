package hu.laci.cms.backend.model.common;

import hu.laci.cms.backend.dao.common.DbColumn;

public abstract class BaseEntity {

    @DbColumn("id")
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
