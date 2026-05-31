package hu.laci.cms.backend.service.security;

/**
 * Rate limiter for failed-attempt lockout workflows.
 */
public interface AttemptRateLimiter {

    /**
     * Checks whether the key is currently locked.
     *
     * @param key limiter key
     * @return true when the key is locked
     */
    boolean isLocked(String key);

    /**
     * Records a failed attempt.
     *
     * @param key limiter key
     */
    void recordFailure(String key);

    /**
     * Clears failed attempt state after a successful operation.
     *
     * @param key limiter key
     */
    void recordSuccess(String key);
}
