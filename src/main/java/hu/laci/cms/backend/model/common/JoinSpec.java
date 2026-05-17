package hu.laci.cms.backend.model.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

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

    public static JoinSpec inner(Class<? extends BaseEntity> entityClass, BaseProperty leftProperty,
                                 BaseProperty rightProperty, BaseProperty targetProperty) {
        return inner(entityClass, leftProperty, rightProperty, targetProperty, null);
    }

    public static JoinSpec inner(Class<? extends BaseEntity> entityClass, BaseProperty leftProperty,
                                 BaseProperty rightProperty, BaseProperty targetProperty, String tableAlias) {
        return new JoinSpec(JoinType.INNER, entityClass, leftProperty, rightProperty, targetProperty, tableAlias);
    }

    public static JoinSpec left(Class<? extends BaseEntity> entityClass, BaseProperty leftProperty,
                                BaseProperty rightProperty, BaseProperty targetProperty) {
        return left(entityClass, leftProperty, rightProperty, targetProperty, null);
    }

    public static JoinSpec left(Class<? extends BaseEntity> entityClass, BaseProperty leftProperty,
                                BaseProperty rightProperty, BaseProperty targetProperty, String tableAlias) {
        return new JoinSpec(JoinType.LEFT, entityClass, leftProperty, rightProperty, targetProperty, tableAlias);
    }

    public JoinType getType() {
        return type;
    }

    public Class<? extends BaseEntity> getEntityClass() {
        return entityClass;
    }

    public BaseProperty getLeftProperty() {
        return leftProperty;
    }

    public BaseProperty getRightProperty() {
        return rightProperty;
    }

    public BaseProperty getTargetProperty() {
        return targetProperty;
    }

    public String getTableAlias() {
        return tableAlias;
    }

    public JoinSpec on(BaseProperty property, FilterOperation operation, Object value) {
        conditions.add(new JoinCondition(property, operation, value, LikeFilterPosition.STARTS_WITH));
        return this;
    }

    public JoinSpec on(BaseProperty property, FilterOperation operation, Object value,
                       LikeFilterPosition likePosition) {
        conditions.add(new JoinCondition(property, operation, value, likePosition));
        return this;
    }

    public JoinConditionBuilder on(BaseProperty property) {
        return new JoinConditionBuilder(this, property);
    }

    public List<JoinCondition> getConditions() {
        return List.copyOf(conditions);
    }

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

        public BaseProperty getProperty() {
            return property;
        }

        public FilterOperation getOperation() {
            return operation;
        }

        public Object getValue() {
            return value;
        }

        public LikeFilterPosition getLikePosition() {
            return likePosition;
        }
    }

    public static final class JoinConditionBuilder {

        private final JoinSpec joinSpec;
        private final BaseProperty property;

        private JoinConditionBuilder(JoinSpec joinSpec, BaseProperty property) {
            this.joinSpec = joinSpec;
            this.property = Objects.requireNonNull(property, "property must not be null");
        }

        public JoinSpec equalsTo(Object value) {
            return joinSpec.on(property, FilterOperation.EQUALS, value);
        }

        public JoinSpec like(String value) {
            return like(value, LikeFilterPosition.STARTS_WITH);
        }

        public JoinSpec like(String value, LikeFilterPosition likePosition) {
            return joinSpec.on(property, FilterOperation.LIKE, value, likePosition);
        }

        public JoinSpec lessThan(Object value) {
            return joinSpec.on(property, FilterOperation.LESS, value);
        }

        public JoinSpec lessThanOrEquals(Object value) {
            return joinSpec.on(property, FilterOperation.LESS_OR_EQUALS, value);
        }

        public JoinSpec greaterThan(Object value) {
            return joinSpec.on(property, FilterOperation.GREATER, value);
        }

        public JoinSpec greaterThanOrEquals(Object value) {
            return joinSpec.on(property, FilterOperation.GREATER_OR_EQUALS, value);
        }

        public JoinSpec in(Collection<?> values) {
            return joinSpec.on(property, FilterOperation.IN, values == null ? List.of() : List.copyOf(values));
        }

        public JoinSpec in(Object... values) {
            return in(values == null ? List.of() : java.util.Arrays.asList(values));
        }

        public JoinSpec notIn(Collection<?> values) {
            return joinSpec.on(property, FilterOperation.NOT_IN, values == null ? List.of() : List.copyOf(values));
        }

        public JoinSpec notIn(Object... values) {
            return notIn(values == null ? List.of() : java.util.Arrays.asList(values));
        }

        public JoinSpec between(Object startValue, Object endValue) {
            return joinSpec.on(property, FilterOperation.BETWEEN, List.of(
                    Objects.requireNonNull(startValue, "startValue must not be null"),
                    Objects.requireNonNull(endValue, "endValue must not be null")));
        }
    }
}
