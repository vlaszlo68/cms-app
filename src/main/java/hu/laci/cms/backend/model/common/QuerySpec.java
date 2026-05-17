package hu.laci.cms.backend.model.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Fluent query description for {@code BaseDao.findAll(...)}.
 * <p>
 * It describes filters, joins, and sort orders without exposing SQL strings to
 * service or servlet code.
 * <p>
 * Basic filtering and sorting:
 *
 * <pre>{@code
 * List<User> users = userDao.findAll(QuerySpec.<UserProperty>create()
 *         .where(UserProperty.LOGIN_NAME).like("admin", LikeFilterPosition.CONTAINS)
 *         .where(UserProperty.ID).greaterThan(10L)
 *         .orderBy(UserProperty.LOGIN_NAME.asc()));
 * }</pre>
 *
 * Multiple filters are combined with SQL {@code AND}. Empty filter values are
 * ignored by {@code BaseDao}, which makes optional form fields easy to pass
 * through:
 *
 * <pre>{@code
 * QuerySpec<UserProperty> query = QuerySpec.<UserProperty>create()
 *         .where(UserProperty.LOGIN_NAME).like(loginNamePrefix)
 *         .where(UserProperty.EMAIL_ADDRESS).like(emailPart, LikeFilterPosition.CONTAINS)
 *         .where(UserProperty.ID).greaterThanOrEquals(minId)
 *         .orderBy(UserProperty.LOGIN_NAME.asc(), UserProperty.ID.desc());
 *
 * List<User> users = userDao.findAll(query);
 * }</pre>
 *
 * Membership and range filters:
 *
 * <pre>{@code
 * List<User> selectedUsers = userDao.findAll(QuerySpec.<UserProperty>create()
 *         .where(UserProperty.ID).in(List.of(1L, 2L, 3L))
 *         .orderBy(UserProperty.ID.desc()));
 *
 * List<User> middleUsers = userDao.findAll(QuerySpec.<UserProperty>create()
 *         .where(UserProperty.ID).between(100L, 200L));
 * }</pre>
 *
 * Direct operation-based form is also available when the operation is selected
 * dynamically:
 *
 * <pre>{@code
 * QuerySpec<UserProperty> query = QuerySpec.<UserProperty>create()
 *         .where(UserProperty.ID, FilterOperation.GREATER_OR_EQUALS, minId)
 *         .where(UserProperty.LOGIN_NAME, FilterOperation.LIKE, "adm", LikeFilterPosition.STARTS_WITH);
 * }</pre>
 *
 * Joining another entity and filtering on the joined table:
 *
 * <pre>{@code
 * List<Child> children = childDao.findAll(QuerySpec.<ChildProperty>create()
 *         .leftJoin(Parent.class, ChildProperty.PARENT, ParentProperty.ID, ChildProperty.PARENT)
 *         .where(ParentProperty.NAME).equalsTo("Main parent")
 *         .orderBy(ChildProperty.NAME.asc()));
 * }</pre>
 *
 * When the same table is joined more than once, use aliases on both the join
 * and the joined-table filter/sort properties:
 *
 * <pre>{@code
 * List<Child> children = childDao.findAll(QuerySpec.<ChildProperty>create()
 *         .leftJoin(Parent.class, ChildProperty.PARENT, ParentProperty.ID,
 *                 ChildProperty.PARENT, "primary_parent")
 *         .leftJoin(Parent.class, ChildProperty.SECOND_PARENT, ParentProperty.ID,
 *                 ChildProperty.SECOND_PARENT, "secondary_parent")
 *         .where(ParentProperty.NAME.withAlias("secondary_parent")).equalsTo("Backup")
 *         .orderBy(ChildProperty.NAME.asc()));
 * }</pre>
 *
 * QuerySpec is intended for entity-list queries. For aggregations, reports,
 * projections, or heavily optimized SQL, define a DAO method and use the custom
 * SQL helpers on {@code BaseDao} instead.
 *
 * @param <P> root entity property type accepted by the DAO
 */
public final class QuerySpec<P extends BaseProperty> {

    private final List<FilterCriterion> filters = new ArrayList<>();
    private final List<SortOrder<P>> sortOrders = new ArrayList<>();
    private final List<JoinSpec> joins = new ArrayList<>();

    private QuerySpec() {
    }

    /**
     * Creates an empty query specification.
     *
     * @param <P> root entity property type
     * @return new mutable query specification
     */
    public static <P extends BaseProperty> QuerySpec<P> create() {
        return new QuerySpec<>();
    }

    /**
     * Adds a filter criterion using the default LIKE position.
     *
     * @param property property to filter on
     * @param operation filter operation
     * @param value filter value; empty values are ignored by {@code BaseDao}
     * @return this query specification
     */
    public QuerySpec<P> where(BaseProperty property, FilterOperation operation, Object value) {
        filters.add(new FilterCriterion(property, operation, value, LikeFilterPosition.STARTS_WITH));
        return this;
    }

    /**
     * Adds a filter criterion with explicit LIKE wildcard placement.
     *
     * @param property property to filter on
     * @param operation filter operation
     * @param value filter value; empty values are ignored by {@code BaseDao}
     * @param likePosition wildcard placement used only for {@link FilterOperation#LIKE}
     * @return this query specification
     */
    public QuerySpec<P> where(BaseProperty property, FilterOperation operation, Object value,
                              LikeFilterPosition likePosition) {
        filters.add(new FilterCriterion(property, operation, value, likePosition));
        return this;
    }

    /**
     * Starts a fluent filter builder for the given property.
     *
     * @param property property to filter on
     * @return filter builder
     */
    public FilterBuilder<P> where(BaseProperty property) {
        return new FilterBuilder<>(this, property);
    }

    /**
     * Adds a join definition to the query.
     *
     * @param join join definition
     * @return this query specification
     */
    public QuerySpec<P> join(JoinSpec join) {
        joins.add(Objects.requireNonNull(join, "join must not be null"));
        return this;
    }

    /**
     * Adds an inner join.
     *
     * @param entityClass joined entity class
     * @param leftProperty property on the already available/root side
     * @param rightProperty property on the joined entity side
     * @param targetProperty entity reference property where the joined row is mapped
     * @return this query specification
     */
    public QuerySpec<P> innerJoin(Class<? extends BaseEntity> entityClass, BaseProperty leftProperty,
                                  BaseProperty rightProperty, BaseProperty targetProperty) {
        return join(JoinSpec.inner(entityClass, leftProperty, rightProperty, targetProperty));
    }

    /**
     * Adds an aliased inner join.
     *
     * @param entityClass joined entity class
     * @param leftProperty property on the already available/root side
     * @param rightProperty property on the joined entity side
     * @param targetProperty entity reference property where the joined row is mapped
     * @param tableAlias SQL alias for the joined table
     * @return this query specification
     */
    public QuerySpec<P> innerJoin(Class<? extends BaseEntity> entityClass, BaseProperty leftProperty,
                                  BaseProperty rightProperty, BaseProperty targetProperty, String tableAlias) {
        return join(JoinSpec.inner(entityClass, leftProperty, rightProperty, targetProperty, tableAlias));
    }

    /**
     * Adds a left join.
     *
     * @param entityClass joined entity class
     * @param leftProperty property on the already available/root side
     * @param rightProperty property on the joined entity side
     * @param targetProperty entity reference property where the joined row is mapped
     * @return this query specification
     */
    public QuerySpec<P> leftJoin(Class<? extends BaseEntity> entityClass, BaseProperty leftProperty,
                                 BaseProperty rightProperty, BaseProperty targetProperty) {
        return join(JoinSpec.left(entityClass, leftProperty, rightProperty, targetProperty));
    }

    /**
     * Adds an aliased left join.
     *
     * @param entityClass joined entity class
     * @param leftProperty property on the already available/root side
     * @param rightProperty property on the joined entity side
     * @param targetProperty entity reference property where the joined row is mapped
     * @param tableAlias SQL alias for the joined table
     * @return this query specification
     */
    public QuerySpec<P> leftJoin(Class<? extends BaseEntity> entityClass, BaseProperty leftProperty,
                                 BaseProperty rightProperty, BaseProperty targetProperty, String tableAlias) {
        return join(JoinSpec.left(entityClass, leftProperty, rightProperty, targetProperty, tableAlias));
    }

    /**
     * Adds ascending order by the given property.
     *
     * @param property property to order by
     * @return this query specification
     */
    public QuerySpec<P> orderBy(P property) {
        sortOrders.add(SortOrder.asc(property));
        return this;
    }

    /**
     * Adds order by the given property and direction.
     *
     * @param property property to order by
     * @param direction direction; defaults to ascending when {@code null}
     * @return this query specification
     */
    public QuerySpec<P> orderBy(P property, SortDirection direction) {
        sortOrders.add(new SortOrder<>(property, direction));
        return this;
    }

    /**
     * Adds one or more sort orders.
     *
     * @param sortOrders sort orders; {@code null} values are ignored
     * @return this query specification
     */
    @SafeVarargs
    public final QuerySpec<P> orderBy(SortOrder<P>... sortOrders) {
        if (sortOrders != null) {
            this.sortOrders.addAll(Arrays.stream(sortOrders)
                    .filter(Objects::nonNull)
                    .toList());
        }
        return this;
    }

    /**
     * Returns an immutable snapshot of filter criteria.
     *
     * @return filter criteria
     */
    public List<FilterCriterion> getFilters() {
        return List.copyOf(filters);
    }

    /**
     * Returns an immutable snapshot of sort orders.
     *
     * @return sort orders
     */
    public List<SortOrder<P>> getSortOrders() {
        return List.copyOf(sortOrders);
    }

    /**
     * Returns an immutable snapshot of joins.
     *
     * @return joins
     */
    public List<JoinSpec> getJoins() {
        return List.copyOf(joins);
    }

    /**
     * Fluent builder for adding one filter criterion.
     *
     * @param <P> root entity property type
     */
    public static final class FilterBuilder<P extends BaseProperty> {

        private final QuerySpec<P> querySpec;
        private final BaseProperty property;

        private FilterBuilder(QuerySpec<P> querySpec, BaseProperty property) {
            this.querySpec = querySpec;
            this.property = Objects.requireNonNull(property, "property must not be null");
        }

        /**
         * Adds an equality filter.
         *
         * @param value expected value
         * @return parent query specification
         */
        public QuerySpec<P> equalsTo(Object value) {
            return querySpec.where(property, FilterOperation.EQUALS, value);
        }

        /**
         * Adds a {@code LIKE value%} filter.
         *
         * @param value text value without SQL wildcards
         * @return parent query specification
         */
        public QuerySpec<P> like(String value) {
            return like(value, LikeFilterPosition.STARTS_WITH);
        }

        /**
         * Adds a LIKE filter with explicit wildcard placement.
         *
         * @param value text value without SQL wildcards
         * @param likePosition wildcard placement
         * @return parent query specification
         */
        public QuerySpec<P> like(String value, LikeFilterPosition likePosition) {
            return querySpec.where(property, FilterOperation.LIKE, value, likePosition);
        }

        /**
         * Adds a less-than filter.
         *
         * @param value upper exclusive bound
         * @return parent query specification
         */
        public QuerySpec<P> lessThan(Object value) {
            return querySpec.where(property, FilterOperation.LESS, value);
        }

        /**
         * Adds a less-than-or-equal filter.
         *
         * @param value upper inclusive bound
         * @return parent query specification
         */
        public QuerySpec<P> lessThanOrEquals(Object value) {
            return querySpec.where(property, FilterOperation.LESS_OR_EQUALS, value);
        }

        /**
         * Adds a greater-than filter.
         *
         * @param value lower exclusive bound
         * @return parent query specification
         */
        public QuerySpec<P> greaterThan(Object value) {
            return querySpec.where(property, FilterOperation.GREATER, value);
        }

        /**
         * Adds a greater-than-or-equal filter.
         *
         * @param value lower inclusive bound
         * @return parent query specification
         */
        public QuerySpec<P> greaterThanOrEquals(Object value) {
            return querySpec.where(property, FilterOperation.GREATER_OR_EQUALS, value);
        }

        /**
         * Adds an {@code IN (...)} filter.
         *
         * @param values allowed values; {@code null} is treated as an empty list
         * @return parent query specification
         */
        public QuerySpec<P> in(Collection<?> values) {
            return querySpec.where(property, FilterOperation.IN, values == null ? List.of() : List.copyOf(values));
        }

        /**
         * Adds an {@code IN (...)} filter.
         *
         * @param values allowed values
         * @return parent query specification
         */
        public QuerySpec<P> in(Object... values) {
            return in(values == null ? List.of() : Arrays.asList(values));
        }

        /**
         * Adds a {@code NOT IN (...)} filter.
         *
         * @param values excluded values; {@code null} is treated as an empty list
         * @return parent query specification
         */
        public QuerySpec<P> notIn(Collection<?> values) {
            return querySpec.where(property, FilterOperation.NOT_IN, values == null ? List.of() : List.copyOf(values));
        }

        /**
         * Adds a {@code NOT IN (...)} filter.
         *
         * @param values excluded values
         * @return parent query specification
         */
        public QuerySpec<P> notIn(Object... values) {
            return notIn(values == null ? List.of() : Arrays.asList(values));
        }

        /**
         * Adds a {@code BETWEEN ? AND ?} filter.
         *
         * @param startValue inclusive start value
         * @param endValue inclusive end value
         * @return parent query specification
         */
        public QuerySpec<P> between(Object startValue, Object endValue) {
            return querySpec.where(property, FilterOperation.BETWEEN, List.of(
                    Objects.requireNonNull(startValue, "startValue must not be null"),
                    Objects.requireNonNull(endValue, "endValue must not be null")));
        }
    }

    /**
     * Immutable filter criterion used by {@link QuerySpec}.
     */
    public static final class FilterCriterion {

        private final BaseProperty property;
        private final FilterOperation operation;
        private final Object value;
        private final LikeFilterPosition likePosition;

        private FilterCriterion(BaseProperty property, FilterOperation operation, Object value,
                                LikeFilterPosition likePosition) {
            this.property = Objects.requireNonNull(property, "property must not be null");
            this.operation = Objects.requireNonNull(operation, "operation must not be null");
            this.value = value;
            this.likePosition = likePosition == null ? LikeFilterPosition.STARTS_WITH : likePosition;
        }

        /**
         * Returns the filtered property.
         *
         * @return property descriptor
         */
        public BaseProperty getProperty() {
            return property;
        }

        /**
         * Returns the filter operation.
         *
         * @return filter operation
         */
        public FilterOperation getOperation() {
            return operation;
        }

        /**
         * Returns the raw filter value.
         *
         * @return filter value
         */
        public Object getValue() {
            return value;
        }

        /**
         * Returns LIKE wildcard placement.
         *
         * @return wildcard placement, meaningful for {@link FilterOperation#LIKE}
         */
        public LikeFilterPosition getLikePosition() {
            return likePosition;
        }
    }
}
