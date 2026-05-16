package hu.laci.cms.backend.model.common;

import java.util.Objects;

public final class SortOrder<S extends BaseSort> {

    private final S sort;
    private final SortDirection direction;

    public SortOrder(S sort) {
        this(sort, SortDirection.ASC);
    }

    public SortOrder(S sort, SortDirection direction) {
        this.sort = Objects.requireNonNull(sort, "sort must not be null");
        this.direction = direction == null ? SortDirection.ASC : direction;
    }

    public static <S extends BaseSort> SortOrder<S> asc(S sort) {
        return new SortOrder<>(sort, SortDirection.ASC);
    }

    public static <S extends BaseSort> SortOrder<S> desc(S sort) {
        return new SortOrder<>(sort, SortDirection.DESC);
    }

    public S getSort() {
        return sort;
    }

    public SortDirection getDirection() {
        return direction;
    }
}
