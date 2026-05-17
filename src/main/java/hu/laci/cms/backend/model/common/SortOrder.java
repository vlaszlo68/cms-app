package hu.laci.cms.backend.model.common;

import java.util.Objects;

public final class SortOrder<P extends BaseProperty> {

    private final P property;
    private final SortDirection direction;

    public SortOrder(P property) {
        this(property, SortDirection.ASC);
    }

    public SortOrder(P property, SortDirection direction) {
        this.property = Objects.requireNonNull(property, "property must not be null");
        this.direction = direction == null ? SortDirection.ASC : direction;
    }

    public static <P extends BaseProperty> SortOrder<P> asc(P property) {
        return new SortOrder<>(property, SortDirection.ASC);
    }

    public static <P extends BaseProperty> SortOrder<P> desc(P property) {
        return new SortOrder<>(property, SortDirection.DESC);
    }

    public P getProperty() {
        return property;
    }

    public SortDirection getDirection() {
        return direction;
    }
}
