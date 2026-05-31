package hu.laci.cms.backend.service.security;

/**
 * Rate limiter for fixed-window request throttling.
 */
public interface RequestRateLimiter {

    /**
     * Records a request and returns whether it is allowed.
     *
     * @param key limiter key
     * @return true when the request is within the configured limit
     */
    boolean allowRequest(String key);
}
