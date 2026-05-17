package hu.laci.cms.backend.model.common;

import java.util.Objects;

/**
 * Sort expression used by {@link QuerySpec}.
 *
 * @param <P> concrete property type of the root query
 */
public final class SortOrder<P extends BaseProperty> {

    private final P property;
    private final SortDirection direction;

    /**
     * Creates ascending sort order for the property.
     *
     * @param property property to order by
     */
    public SortOrder(P property) {
        this(property, SortDirection.ASC);
    }

    /**
     * Creates sort order for the property.
     *
     * @param property property to order by
     * @param direction sort direction; defaults to {@link SortDirection#ASC} when {@code null}
     */
    public SortOrder(P property, SortDirection direction) {
        this.property = Objects.requireNonNull(property, "property must not be null");
        this.direction = direction == null ? SortDirection.ASC : direction;
    }

    /**
     * Creates ascending sort order.
     *
     * @param property property to order by
     * @param <P> concrete property type
     * @return ascending sort order
     */
    public static <P extends BaseProperty> SortOrder<P> asc(P property) {
        return new SortOrder<>(property, SortDirection.ASC);
    }

    /**
     * Creates descending sort order.
     *
     * @param property property to order by
     * @param <P> concrete property type
     * @return descending sort order
     */
    public static <P extends BaseProperty> SortOrder<P> desc(P property) {
        return new SortOrder<>(property, SortDirection.DESC);
    }

    /**
     * Returns the property to order by.
     *
     * @return sort property
     */
    public P getProperty() {
        return property;
    }

    /**
     * Returns the sort direction.
     *
     * @return sort direction
     */
    public SortDirection getDirection() {
        return direction;
    }
}
