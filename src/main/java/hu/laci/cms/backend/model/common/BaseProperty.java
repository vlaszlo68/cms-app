package hu.laci.cms.backend.model.common;

public class BaseProperty {

    public static final BaseProperty ID = new BaseProperty("id");

    private final String propertyName;

    protected BaseProperty(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getPropertyName() {
        return propertyName;
    }

    @SuppressWarnings("unchecked")
    public <P extends BaseProperty> SortOrder<P> asc() {
        return SortOrder.asc((P) this);
    }

    @SuppressWarnings("unchecked")
    public <P extends BaseProperty> SortOrder<P> desc() {
        return SortOrder.desc((P) this);
    }
}
