package hu.laci.cms.backend.model.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public final class QuerySpec<P extends BaseProperty> {

    private final List<FilterCriterion> filters = new ArrayList<>();
    private final List<SortOrder<P>> sortOrders = new ArrayList<>();
    private final List<JoinSpec> joins = new ArrayList<>();

    private QuerySpec() {
    }

    public static <P extends BaseProperty> QuerySpec<P> create() {
        return new QuerySpec<>();
    }

    public QuerySpec<P> where(BaseProperty property, FilterOperation operation, Object value) {
        filters.add(new FilterCriterion(property, operation, value, LikeFilterPosition.STARTS_WITH));
        return this;
    }

    public QuerySpec<P> where(BaseProperty property, FilterOperation operation, Object value,
                              LikeFilterPosition likePosition) {
        filters.add(new FilterCriterion(property, operation, value, likePosition));
        return this;
    }

    public FilterBuilder<P> where(BaseProperty property) {
        return new FilterBuilder<>(this, property);
    }

    public QuerySpec<P> join(JoinSpec join) {
        joins.add(Objects.requireNonNull(join, "join must not be null"));
        return this;
    }

    public QuerySpec<P> innerJoin(Class<? extends BaseEntity> entityClass, BaseProperty leftProperty,
                                  BaseProperty rightProperty, BaseProperty targetProperty) {
        return join(JoinSpec.inner(entityClass, leftProperty, rightProperty, targetProperty));
    }

    public QuerySpec<P> innerJoin(Class<? extends BaseEntity> entityClass, BaseProperty leftProperty,
                                  BaseProperty rightProperty, BaseProperty targetProperty, String tableAlias) {
        return join(JoinSpec.inner(entityClass, leftProperty, rightProperty, targetProperty, tableAlias));
    }

    public QuerySpec<P> leftJoin(Class<? extends BaseEntity> entityClass, BaseProperty leftProperty,
                                 BaseProperty rightProperty, BaseProperty targetProperty) {
        return join(JoinSpec.left(entityClass, leftProperty, rightProperty, targetProperty));
    }

    public QuerySpec<P> leftJoin(Class<? extends BaseEntity> entityClass, BaseProperty leftProperty,
                                 BaseProperty rightProperty, BaseProperty targetProperty, String tableAlias) {
        return join(JoinSpec.left(entityClass, leftProperty, rightProperty, targetProperty, tableAlias));
    }

    public QuerySpec<P> orderBy(P property) {
        sortOrders.add(SortOrder.asc(property));
        return this;
    }

    public QuerySpec<P> orderBy(P property, SortDirection direction) {
        sortOrders.add(new SortOrder<>(property, direction));
        return this;
    }

    @SafeVarargs
    public final QuerySpec<P> orderBy(SortOrder<P>... sortOrders) {
        if (sortOrders != null) {
            this.sortOrders.addAll(Arrays.stream(sortOrders)
                    .filter(Objects::nonNull)
                    .toList());
        }
        return this;
    }

    public List<FilterCriterion> getFilters() {
        return List.copyOf(filters);
    }

    public List<SortOrder<P>> getSortOrders() {
        return List.copyOf(sortOrders);
    }

    public List<JoinSpec> getJoins() {
        return List.copyOf(joins);
    }

    public static final class FilterBuilder<P extends BaseProperty> {

        private final QuerySpec<P> querySpec;
        private final BaseProperty property;

        private FilterBuilder(QuerySpec<P> querySpec, BaseProperty property) {
            this.querySpec = querySpec;
            this.property = Objects.requireNonNull(property, "property must not be null");
        }

        public QuerySpec<P> equalsTo(Object value) {
            return querySpec.where(property, FilterOperation.EQUALS, value);
        }

        public QuerySpec<P> like(String value) {
            return like(value, LikeFilterPosition.STARTS_WITH);
        }

        public QuerySpec<P> like(String value, LikeFilterPosition likePosition) {
            return querySpec.where(property, FilterOperation.LIKE, value, likePosition);
        }

        public QuerySpec<P> lessThan(Object value) {
            return querySpec.where(property, FilterOperation.LESS, value);
        }

        public QuerySpec<P> lessThanOrEquals(Object value) {
            return querySpec.where(property, FilterOperation.LESS_OR_EQUALS, value);
        }

        public QuerySpec<P> greaterThan(Object value) {
            return querySpec.where(property, FilterOperation.GREATER, value);
        }

        public QuerySpec<P> greaterThanOrEquals(Object value) {
            return querySpec.where(property, FilterOperation.GREATER_OR_EQUALS, value);
        }

        public QuerySpec<P> in(Collection<?> values) {
            return querySpec.where(property, FilterOperation.IN, values == null ? List.of() : List.copyOf(values));
        }

        public QuerySpec<P> in(Object... values) {
            return in(values == null ? List.of() : Arrays.asList(values));
        }

        public QuerySpec<P> notIn(Collection<?> values) {
            return querySpec.where(property, FilterOperation.NOT_IN, values == null ? List.of() : List.copyOf(values));
        }

        public QuerySpec<P> notIn(Object... values) {
            return notIn(values == null ? List.of() : Arrays.asList(values));
        }

        public QuerySpec<P> between(Object startValue, Object endValue) {
            return querySpec.where(property, FilterOperation.BETWEEN, List.of(
                    Objects.requireNonNull(startValue, "startValue must not be null"),
                    Objects.requireNonNull(endValue, "endValue must not be null")));
        }
    }

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
}
