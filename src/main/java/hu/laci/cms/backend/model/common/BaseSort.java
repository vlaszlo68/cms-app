package hu.laci.cms.backend.model.common;

import hu.laci.cms.backend.dao.common.SortProperty;

public class BaseSort implements SortProperty {

    public static final BaseSort ID = new BaseSort("id");

    private final String propertyName;

    protected BaseSort(String propertyName) {
        this.propertyName = propertyName;
    }

    @Override
    public String getPropertyName() {
        return propertyName;
    }

    @SuppressWarnings("unchecked")
    public <S extends BaseSort> SortOrder<S> asc() {
        return SortOrder.asc((S) this);
    }

    @SuppressWarnings("unchecked")
    public <S extends BaseSort> SortOrder<S> desc() {
        return SortOrder.desc((S) this);
    }
}
