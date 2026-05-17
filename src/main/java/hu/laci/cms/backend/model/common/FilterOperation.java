package hu.laci.cms.backend.model.common;

/**
 * Supported filter operations for {@link QuerySpec} and extra {@link JoinSpec}
 * {@code ON} conditions.
 */
public enum FilterOperation {
    /**
     * SQL equality: {@code column = ?}.
     */
    EQUALS,
    /**
     * SQL {@code LIKE}; wildcard placement is controlled by {@link LikeFilterPosition}.
     */
    LIKE,
    /**
     * SQL less-than comparison: {@code column < ?}.
     */
    LESS,
    /**
     * SQL less-than-or-equal comparison: {@code column <= ?}.
     */
    LESS_OR_EQUALS,
    /**
     * SQL greater-than comparison: {@code column > ?}.
     */
    GREATER,
    /**
     * SQL greater-than-or-equal comparison: {@code column >= ?}.
     */
    GREATER_OR_EQUALS,
    /**
     * SQL membership test: {@code column IN (...)}.
     */
    IN,
    /**
     * SQL negative membership test: {@code column NOT IN (...)}.
     */
    NOT_IN,
    /**
     * SQL range test with two values: {@code column BETWEEN ? AND ?}.
     */
    BETWEEN
}
