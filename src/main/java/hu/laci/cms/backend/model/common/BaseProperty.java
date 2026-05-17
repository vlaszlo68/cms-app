package hu.laci.cms.backend.model.common;

public class BaseProperty {

    public static final BaseProperty ID = new BaseProperty("id");

    private final Class<? extends BaseEntity> entityClass;
    private final String propertyName;
    private final String tableAlias;

    protected BaseProperty(String propertyName) {
        this(null, propertyName);
    }

    protected BaseProperty(Class<? extends BaseEntity> entityClass, String propertyName) {
        this(entityClass, propertyName, null);
    }

    protected BaseProperty(Class<? extends BaseEntity> entityClass, String propertyName, String tableAlias) {
        this.entityClass = entityClass;
        this.propertyName = propertyName;
        this.tableAlias = tableAlias;
    }

    public Class<? extends BaseEntity> getEntityClass() {
        return entityClass;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getTableAlias() {
        return tableAlias;
    }

    public BaseProperty withAlias(String tableAlias) {
        return new BaseProperty(entityClass, propertyName, tableAlias);
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
