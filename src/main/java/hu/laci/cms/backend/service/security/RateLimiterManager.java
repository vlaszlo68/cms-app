package hu.laci.cms.backend.service.security;

import java.time.Duration;

/**
 * Factory facade for rate limiter implementations.
 */
public final class RateLimiterManager {

    private static volatile RateLimiterConfig config = RateLimiterConfig.getCurrent();

    private RateLimiterManager() {
    }

    /**
     * Initializes the manager with resolved configuration.
     *
     * @param rateLimiterConfig rate limiter configuration
     */
    public static void initialize(RateLimiterConfig rateLimiterConfig) {
        config = rateLimiterConfig;
    }

    public static void reset() {
        config = RateLimiterConfig.getCurrent();
    }

    /**
     * Creates a failed-attempt limiter.
     *
     * @param limiterName limiter namespace
     * @param maxFailures maximum failures before lockout
     * @param lockDuration lockout duration
     * @return limiter implementation
     */
    public static AttemptRateLimiter createAttemptLimiter(String limiterName, int maxFailures,
                                                          Duration lockDuration) {
        return switch (config.getStoreMode()) {
            case MEMORY -> new InMemoryRateLimiter(maxFailures, lockDuration);
            case JDBC -> new JdbcAttemptRateLimiter(limiterName, maxFailures, lockDuration);
            case REDIS -> throw new IllegalStateException("Redis rate limiter store is planned but not implemented.");
        };
    }

    /**
     * Creates a fixed-window request limiter.
     *
     * @param limiterName limiter namespace
     * @param maxRequests maximum requests in one window
     * @param windowDuration window duration
     * @return limiter implementation
     */
    public static RequestRateLimiter createRequestLimiter(String limiterName, int maxRequests,
                                                          Duration windowDuration) {
        return switch (config.getStoreMode()) {
            case MEMORY -> new InMemoryRequestRateLimiter(maxRequests, windowDuration);
            case JDBC -> new JdbcRequestRateLimiter(limiterName, maxRequests, windowDuration);
            case REDIS -> throw new IllegalStateException("Redis rate limiter store is planned but not implemented.");
        };
    }
}
