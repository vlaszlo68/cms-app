package hu.laci.cms.backend.model.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Join description used by {@link QuerySpec}.
 * <p>
 * A join defines both the SQL join condition and where the joined entity should
 * be mapped on the already loaded object graph. Mapping is explicit through
 * {@code targetProperty}; the DAO does not infer it automatically.
 * <p>
 * Simple left join from a child entity to its parent:
 *
 * <pre>{@code
 * QuerySpec.<ChildProperty>create()
 *         .leftJoin(Parent.class, ChildProperty.PARENT, ParentProperty.ID, ChildProperty.PARENT)
 *         .where(ParentProperty.NAME).equalsTo("Main");
 * }</pre>
 *
 * Joining the same entity type twice requires explicit SQL aliases. Filters and
 * sorts targeting a specific joined table must use the same alias on the
 * property:
 *
 * <pre>{@code
 * QuerySpec.<ChildProperty>create()
 *         .leftJoin(Parent.class, ChildProperty.PARENT, ParentProperty.ID,
 *                 ChildProperty.PARENT, "primary_parent")
 *         .leftJoin(Parent.class, ChildProperty.SECOND_PARENT, ParentProperty.ID,
 *                 ChildProperty.SECOND_PARENT, "secondary_parent")
 *         .where(ParentProperty.NAME.withAlias("secondary_parent")).equalsTo("Backup");
 * }</pre>
 *
 * Nested joins are supported when the intermediate joined entity has already
 * been mapped. In this example the parent is mapped to {@code child.parent},
 * then the grandparent is mapped to {@code child.parent.grandParent}:
 *
 * <pre>{@code
 * QuerySpec.<ChildProperty>create()
 *         .leftJoin(Parent.class, ChildProperty.PARENT, ParentProperty.ID,
 *                 ChildProperty.PARENT, "parent_alias")
 *         .leftJoin(GrandParent.class, ParentProperty.GRAND_PARENT, GrandParentProperty.ID,
 *                 ParentProperty.GRAND_PARENT)
 *         .where(ParentProperty.NAME.withAlias("parent_alias")).like("Admin");
 * }</pre>
 *
 * Extra conditions can be added to the join {@code ON} clause. These conditions
 * are applied while joining, not as root-level {@code WHERE} filters:
 *
 * <pre>{@code
 * JoinSpec visibleParentJoin = JoinSpec.left(Parent.class,
 *         ChildProperty.PARENT,
 *         ParentProperty.ID,
 *         ChildProperty.PARENT,
 *         "visible_parent")
 *         .on(ParentProperty.VISIBLE.withAlias("visible_parent")).equalsTo(true)
 *         .on(ParentProperty.NAME.withAlias("visible_parent"))
 *         .like("Main", LikeFilterPosition.CONTAINS);
 *
 * QuerySpec.<ChildProperty>create()
 *         .join(visibleParentJoin);
 * }</pre>
 */
public final class JoinSpec {

    private final JoinType type;
    private final Class<? extends BaseEntity> entityClass;
    private final BaseProperty leftProperty;
    private final BaseProperty rightProperty;
    private final BaseProperty targetProperty;
    private final String tableAlias;
    private final List<JoinCondition> conditions = new ArrayList<>();

    private JoinSpec(JoinType type, Class<? extends BaseEntity> entityClass, BaseProperty leftProperty,
                     BaseProperty rightProperty, BaseProperty targetProperty, String tableAlias) {
        this.type = type == null ? JoinType.INNER : type;
        this.entityClass = Objects.requireNonNull(entityClass, "entityClass must not be null");
        this.leftProperty = Objects.requireNonNull(leftProperty, "leftProperty must not be null");
        this.rightProperty = Objects.requireNonNull(rightProperty, "rightProperty must not be null");
        this.targetProperty = Objects.requireNonNull(targetProperty, "targetProperty must not be null");
        this.tableAlias = tableAlias;
    }

    /**
     * Creates an inner join without explicit SQL alias.
     *
     * @param entityClass joined entity class
     * @param leftProperty property on the already available/root side
     * @param rightProperty property on the joined entity side
     * @param targetProperty entity reference property where the joined row is mapped
     * @return join specification
     */
    public static JoinSpec inner(Class<? extends BaseEntity> entityClass, BaseProperty leftProperty,
                                 BaseProperty rightProperty, BaseProperty targetProperty) {
        return inner(entityClass, leftProperty, rightProperty, targetProperty, null);
    }

    /**
     * Creates an inner join with optional SQL alias.
     *
     * @param entityClass joined entity class
     * @param leftProperty property on the already available/root side
     * @param rightProperty property on the joined entity side
     * @param targetProperty entity reference property where the joined row is mapped
     * @param tableAlias SQL alias; required when the same entity type is joined more than once
     * @return join specification
     */
    public static JoinSpec inner(Class<? extends BaseEntity> entityClass, BaseProperty leftProperty,
                                 BaseProperty rightProperty, BaseProperty targetProperty, String tableAlias) {
        return new JoinSpec(JoinType.INNER, entityClass, leftProperty, rightProperty, targetProperty, tableAlias);
    }

    /**
     * Creates a left join without explicit SQL alias.
     *
     * @param entityClass joined entity class
     * @param leftProperty property on the already available/root side
     * @param rightProperty property on the joined entity side
     * @param targetProperty entity reference property where the joined row is mapped
     * @return join specification
     */
    public static JoinSpec left(Class<? extends BaseEntity> entityClass, BaseProperty leftProperty,
                                BaseProperty rightProperty, BaseProperty targetProperty) {
        return left(entityClass, leftProperty, rightProperty, targetProperty, null);
    }

    /**
     * Creates a left join with optional SQL alias.
     *
     * @param entityClass joined entity class
     * @param leftProperty property on the already available/root side
     * @param rightProperty property on the joined entity side
     * @param targetProperty entity reference property where the joined row is mapped
     * @param tableAlias SQL alias; required when the same entity type is joined more than once
     * @return join specification
     */
    public static JoinSpec left(Class<? extends BaseEntity> entityClass, BaseProperty leftProperty,
                                BaseProperty rightProperty, BaseProperty targetProperty, String tableAlias) {
        return new JoinSpec(JoinType.LEFT, entityClass, leftProperty, rightProperty, targetProperty, tableAlias);
    }

    /**
     * Returns the SQL join type.
     *
     * @return join type
     */
    public JoinType getType() {
        return type;
    }

    /**
     * Returns the joined entity class.
     *
     * @return joined entity class
     */
    public Class<? extends BaseEntity> getEntityClass() {
        return entityClass;
    }

    /**
     * Returns the property used on the already available/root side of the join.
     *
     * @return left-side property
     */
    public BaseProperty getLeftProperty() {
        return leftProperty;
    }

    /**
     * Returns the property used on the joined entity side of the join.
     *
     * @return right-side property
     */
    public BaseProperty getRightProperty() {
        return rightProperty;
    }

    /**
     * Returns the entity reference property that receives the joined object.
     *
     * @return target mapping property
     */
    public BaseProperty getTargetProperty() {
        return targetProperty;
    }

    /**
     * Returns the explicit SQL alias for the joined table.
     *
     * @return SQL alias, or {@code null}
     */
    public String getTableAlias() {
        return tableAlias;
    }

    /**
     * Adds an extra condition to the join {@code ON} clause.
     * <p>
     * The key equality remains part of the join; this method appends an
     * additional {@code AND ...} condition.
     *
     * @param property property to constrain in the {@code ON} clause
     * @param operation filter operation
     * @param value filter value
     * @return this join specification
     */
    public JoinSpec on(BaseProperty property, FilterOperation operation, Object value) {
        conditions.add(new JoinCondition(property, operation, value, LikeFilterPosition.STARTS_WITH));
        return this;
    }

    /**
     * Adds an extra condition to the join {@code ON} clause with explicit LIKE
     * wildcard placement.
     *
     * @param property property to constrain in the {@code ON} clause
     * @param operation filter operation
     * @param value filter value
     * @param likePosition wildcard placement used for {@link FilterOperation#LIKE}
     * @return this join specification
     */
    public JoinSpec on(BaseProperty property, FilterOperation operation, Object value,
                       LikeFilterPosition likePosition) {
        conditions.add(new JoinCondition(property, operation, value, likePosition));
        return this;
    }

    /**
     * Starts a fluent builder for an extra join {@code ON} condition.
     * <p>
     * Example:
     *
     * <pre>{@code
     * JoinSpec.left(Parent.class, ChildProperty.PARENT, ParentProperty.ID, ChildProperty.PARENT, "p")
     *         .on(ParentProperty.NAME.withAlias("p")).equalsTo("Visible parent");
     * }</pre>
     *
     * @param property property to constrain
     * @return condition builder
     */
    public JoinConditionBuilder on(BaseProperty property) {
        return new JoinConditionBuilder(this, property);
    }

    /**
     * Returns an immutable snapshot of extra {@code ON} conditions.
     *
     * @return extra join conditions
     */
    public List<JoinCondition> getConditions() {
        return List.copyOf(conditions);
    }

    /**
     * Extra filter condition appended to a join {@code ON} clause.
     */
    public static final class JoinCondition {

        private final BaseProperty property;
        private final FilterOperation operation;
        private final Object value;
        private final LikeFilterPosition likePosition;

        private JoinCondition(BaseProperty property, FilterOperation operation, Object value,
                              LikeFilterPosition likePosition) {
            this.property = Objects.requireNonNull(property, "property must not be null");
            this.operation = Objects.requireNonNull(operation, "operation must not be null");
            this.value = value;
            this.likePosition = likePosition == null ? LikeFilterPosition.STARTS_WITH : likePosition;
        }

        /**
         * Returns the constrained property.
         *
         * @return property descriptor
         */
        public BaseProperty getProperty() {
            return property;
        }

        /**
         * Returns the condition operation.
         *
         * @return filter operation
         */
        public FilterOperation getOperation() {
            return operation;
        }

        /**
         * Returns the raw condition value.
         *
         * @return condition value
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

    /**
     * Fluent builder for one extra join {@code ON} condition.
     */
    public static final class JoinConditionBuilder {

        private final JoinSpec joinSpec;
        private final BaseProperty property;

        private JoinConditionBuilder(JoinSpec joinSpec, BaseProperty property) {
            this.joinSpec = joinSpec;
            this.property = Objects.requireNonNull(property, "property must not be null");
        }

        /**
         * Adds an equality condition.
         *
         * @param value expected value
         * @return parent join specification
         */
        public JoinSpec equalsTo(Object value) {
            return joinSpec.on(property, FilterOperation.EQUALS, value);
        }

        /**
         * Adds a {@code LIKE value%} condition.
         *
         * @param value text value without SQL wildcards
         * @return parent join specification
         */
        public JoinSpec like(String value) {
            return like(value, LikeFilterPosition.STARTS_WITH);
        }

        /**
         * Adds a LIKE condition with explicit wildcard placement.
         *
         * @param value text value without SQL wildcards
         * @param likePosition wildcard placement
         * @return parent join specification
         */
        public JoinSpec like(String value, LikeFilterPosition likePosition) {
            return joinSpec.on(property, FilterOperation.LIKE, value, likePosition);
        }

        /**
         * Adds a less-than condition.
         *
         * @param value upper exclusive bound
         * @return parent join specification
         */
        public JoinSpec lessThan(Object value) {
            return joinSpec.on(property, FilterOperation.LESS, value);
        }

        /**
         * Adds a less-than-or-equal condition.
         *
         * @param value upper inclusive bound
         * @return parent join specification
         */
        public JoinSpec lessThanOrEquals(Object value) {
            return joinSpec.on(property, FilterOperation.LESS_OR_EQUALS, value);
        }

        /**
         * Adds a greater-than condition.
         *
         * @param value lower exclusive bound
         * @return parent join specification
         */
        public JoinSpec greaterThan(Object value) {
            return joinSpec.on(property, FilterOperation.GREATER, value);
        }

        /**
         * Adds a greater-than-or-equal condition.
         *
         * @param value lower inclusive bound
         * @return parent join specification
         */
        public JoinSpec greaterThanOrEquals(Object value) {
            return joinSpec.on(property, FilterOperation.GREATER_OR_EQUALS, value);
        }

        /**
         * Adds an {@code IN (...)} condition.
         *
         * @param values allowed values; {@code null} is treated as an empty list
         * @return parent join specification
         */
        public JoinSpec in(Collection<?> values) {
            return joinSpec.on(property, FilterOperation.IN, values == null ? List.of() : List.copyOf(values));
        }

        /**
         * Adds an {@code IN (...)} condition.
         *
         * @param values allowed values
         * @return parent join specification
         */
        public JoinSpec in(Object... values) {
            return in(values == null ? List.of() : java.util.Arrays.asList(values));
        }

        /**
         * Adds a {@code NOT IN (...)} condition.
         *
         * @param values excluded values; {@code null} is treated as an empty list
         * @return parent join specification
         */
        public JoinSpec notIn(Collection<?> values) {
            return joinSpec.on(property, FilterOperation.NOT_IN, values == null ? List.of() : List.copyOf(values));
        }

        /**
         * Adds a {@code NOT IN (...)} condition.
         *
         * @param values excluded values
         * @return parent join specification
         */
        public JoinSpec notIn(Object... values) {
            return notIn(values == null ? List.of() : java.util.Arrays.asList(values));
        }

        /**
         * Adds a {@code BETWEEN ? AND ?} condition.
         *
         * @param startValue inclusive start value
         * @param endValue inclusive end value
         * @return parent join specification
         */
        public JoinSpec between(Object startValue, Object endValue) {
            return joinSpec.on(property, FilterOperation.BETWEEN, List.of(
                    Objects.requireNonNull(startValue, "startValue must not be null"),
                    Objects.requireNonNull(endValue, "endValue must not be null")));
        }
    }
}
