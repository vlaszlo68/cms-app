package hu.laci.cms.backend.model.common;

import hu.laci.cms.backend.model.common.annotations.FilterProperty;

public class BaseFilter {

    @FilterProperty(entityProperty = "id")
    private Long id;

    public BaseFilter() {
    }

    public BaseFilter(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
