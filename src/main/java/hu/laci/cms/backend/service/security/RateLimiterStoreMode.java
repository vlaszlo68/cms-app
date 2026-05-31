package hu.laci.cms.backend.service.security;

/**
 * Supported backing stores for rate limiter state.
 */
public enum RateLimiterStoreMode {

    MEMORY,
    JDBC,
    REDIS;

    /**
     * Parses a configured rate limiter store mode.
     *
     * @param value configured value
     * @return parsed mode
     */
    public static RateLimiterStoreMode parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Rate limiter store mode must not be blank.");
        }

        return RateLimiterStoreMode.valueOf(value.trim().toUpperCase());
    }
}
