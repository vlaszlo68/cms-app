package hu.laci.cms.backend.model.common;

/**
 * Type-safe descriptor for an entity property used by {@link QuerySpec},
 * {@link JoinSpec}, and {@link SortOrder}.
 * <p>
 * Concrete entity-specific property classes usually expose constants:
 *
 * <pre>{@code
 * public final class UserProperty extends BaseProperty {
 *     public static final UserProperty LOGIN_NAME = new UserProperty("loginName");
 *
 *     private UserProperty(String propertyName) {
 *         super(User.class, propertyName);
 *     }
 * }
 * }</pre>
 */
public class BaseProperty {

    /**
     * Common id property for root-entity queries.
     */
    public static final BaseProperty ID = new BaseProperty("id");

    private final Class<? extends BaseEntity> entityClass;
    private final String propertyName;
    private final String tableAlias;

    /**
     * Creates a property descriptor without a fixed entity class.
     *
     * @param propertyName Java field name on the queried entity
     */
    protected BaseProperty(String propertyName) {
        this(null, propertyName);
    }

    /**
     * Creates a property descriptor bound to an entity class.
     *
     * @param entityClass entity class that owns the property
     * @param propertyName Java field name on that entity
     */
    protected BaseProperty(Class<? extends BaseEntity> entityClass, String propertyName) {
        this(entityClass, propertyName, null);
    }

    /**
     * Creates a property descriptor bound to an entity class and SQL table alias.
     * <p>
     * The alias is needed when the same entity type is joined multiple times or
     * when a filter/sort must target a specific joined table.
     *
     * @param entityClass entity class that owns the property
     * @param propertyName Java field name on that entity
     * @param tableAlias SQL alias used for the joined table
     */
    protected BaseProperty(Class<? extends BaseEntity> entityClass, String propertyName, String tableAlias) {
        this.entityClass = entityClass;
        this.propertyName = propertyName;
        this.tableAlias = tableAlias;
    }

    /**
     * Returns the entity class that owns this property.
     *
     * @return owner entity class, or {@code null} when the property should be resolved against the root entity
     */
    public Class<? extends BaseEntity> getEntityClass() {
        return entityClass;
    }

    /**
     * Returns the Java field name.
     *
     * @return property name, for example {@code loginName}
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * Returns the SQL table alias tied to this property.
     *
     * @return table alias, or {@code null} when no explicit alias is required
     */
    public String getTableAlias() {
        return tableAlias;
    }

    /**
     * Creates a copy of this property targeting the given SQL table alias.
     * <p>
     * Example:
     *
     * <pre>{@code
     * QuerySpec.<ChildProperty>create()
     *         .where(ParentProperty.NAME.withAlias("secondary_parent"))
     *         .equalsTo("Main parent");
     * }</pre>
     *
     * @param tableAlias SQL alias used in the query
     * @return copied property descriptor with the given alias
     */
    public BaseProperty withAlias(String tableAlias) {
        return new BaseProperty(entityClass, propertyName, tableAlias);
    }

    /**
     * Creates ascending sort order for this property.
     *
     * @param <P> concrete property type expected by the query
     * @return ascending sort order
     */
    @SuppressWarnings("unchecked")
    public <P extends BaseProperty> SortOrder<P> asc() {
        return SortOrder.asc((P) this);
    }

    /**
     * Creates descending sort order for this property.
     *
     * @param <P> concrete property type expected by the query
     * @return descending sort order
     */
    @SuppressWarnings("unchecked")
    public <P extends BaseProperty> SortOrder<P> desc() {
        return SortOrder.desc((P) this);
    }
}
