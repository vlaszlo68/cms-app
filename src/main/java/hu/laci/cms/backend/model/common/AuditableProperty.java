package hu.laci.cms.backend.model.common;

/**
 * Common property descriptors for entities extending {@link AuditableEntity}.
 */
public class AuditableProperty extends BaseProperty {

    public static final AuditableProperty CREATED_AT = new AuditableProperty("createdAt");
    public static final AuditableProperty UPDATED_AT = new AuditableProperty("updatedAt");
    public static final AuditableProperty CREATED_BY = new AuditableProperty("createdBy");
    public static final AuditableProperty UPDATED_BY = new AuditableProperty("updatedBy");

    /**
     * Creates an auditable property descriptor without a fixed entity class.
     *
     * @param propertyName Java field name on the queried auditable entity
     */
    protected AuditableProperty(String propertyName) {
        super(propertyName);
    }

    /**
     * Creates an auditable property descriptor bound to an entity class.
     *
     * @param entityClass auditable entity class that owns the property
     * @param propertyName Java field name on that entity
     */
    protected AuditableProperty(Class<? extends AuditableEntity> entityClass, String propertyName) {
        super(entityClass, propertyName);
    }

    /**
     * Creates an auditable property descriptor bound to an entity class and SQL table alias.
     *
     * @param entityClass auditable entity class that owns the property
     * @param propertyName Java field name on that entity
     * @param tableAlias SQL alias used for the joined table
     */
    protected AuditableProperty(Class<? extends AuditableEntity> entityClass, String propertyName, String tableAlias) {
        super(entityClass, propertyName, tableAlias);
    }
}
