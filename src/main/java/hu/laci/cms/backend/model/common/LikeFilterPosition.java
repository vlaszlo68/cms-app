package hu.laci.cms.backend.model.common;

/**
 * Controls where {@code %} wildcards are added for {@link FilterOperation#LIKE}.
 */
public enum LikeFilterPosition {
    /**
     * Matches values starting with the given text: {@code value%}.
     */
    STARTS_WITH,
    /**
     * Matches values ending with the given text: {@code %value}.
     */
    ENDS_WITH,
    /**
     * Matches values containing the given text: {@code %value%}.
     */
    CONTAINS
}
