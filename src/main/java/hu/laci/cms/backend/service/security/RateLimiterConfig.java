package hu.laci.cms.backend.service.security;

import hu.laci.cms.backend.config.app.ServletContextParameters;

import javax.servlet.ServletContext;
import java.util.Map;

/**
 * Environment-first configuration for rate limiter backing stores.
 */
public final class RateLimiterConfig {

    public static final String ENV_STORE_MODE = "RATE_LIMITER_STORE_MODE";
    public static final String PARAM_STORE_MODE = "rateLimiter.store.mode";

    private static final RateLimiterConfig DEFAULT = new RateLimiterConfig(RateLimiterStoreMode.MEMORY);
    private static volatile RateLimiterConfig current = DEFAULT;

    private final RateLimiterStoreMode storeMode;

    private RateLimiterConfig(RateLimiterStoreMode storeMode) {
        this.storeMode = storeMode;
    }

    /**
     * Initializes rate limiter configuration.
     *
     * @param servletContext servlet context used for web.xml fallback
     */
    public static void initialize(ServletContext servletContext) {
        current = from(servletContext, System.getenv());
    }

    public static void reset() {
        current = DEFAULT;
    }

    public static RateLimiterConfig getCurrent() {
        return current;
    }

    static RateLimiterConfig from(ServletContext servletContext, Map<String, String> environment) {
        String configuredMode = environment.get(ENV_STORE_MODE);
        if (configuredMode == null || configuredMode.isBlank()) {
            configuredMode = ServletContextParameters.getString(servletContext, PARAM_STORE_MODE);
        }

        RateLimiterStoreMode mode = configuredMode == null || configuredMode.isBlank()
                ? RateLimiterStoreMode.MEMORY
                : RateLimiterStoreMode.parse(configuredMode);
        return new RateLimiterConfig(mode);
    }

    public RateLimiterStoreMode getStoreMode() {
        return storeMode;
    }
}
