package hu.laci.cms.backend.service.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletContext;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.Map;

/** Unit tests for rate-limiter store-mode parsing and limiter implementation selection. */
class RateLimiterManagerTest {

    @AfterEach
    void resetManager() {
        RateLimiterConfig.reset();
        RateLimiterManager.reset();
    }

    @Test
    void parsesConfiguredStoreModesCaseInsensitively() {
        Assertions.assertEquals(RateLimiterStoreMode.MEMORY, RateLimiterStoreMode.parse("memory"));
        Assertions.assertEquals(RateLimiterStoreMode.JDBC, RateLimiterStoreMode.parse(" JDBC "));
        Assertions.assertThrows(IllegalArgumentException.class, () -> RateLimiterStoreMode.parse("unknown"));
    }

    @Test
    void managerCreatesMemoryAndJdbcImplementationsFromConfiguration() {
        RateLimiterManager.initialize(config("memory"));
        Assertions.assertInstanceOf(InMemoryRateLimiter.class,
                RateLimiterManager.createAttemptLimiter("test", 2, Duration.ofMinutes(1)));
        Assertions.assertInstanceOf(InMemoryRequestRateLimiter.class,
                RateLimiterManager.createRequestLimiter("test", 2, Duration.ofMinutes(1)));

        RateLimiterManager.initialize(config("jdbc"));
        Assertions.assertInstanceOf(JdbcAttemptRateLimiter.class,
                RateLimiterManager.createAttemptLimiter("test", 2, Duration.ofMinutes(1)));
        Assertions.assertInstanceOf(JdbcRequestRateLimiter.class,
                RateLimiterManager.createRequestLimiter("test", 2, Duration.ofMinutes(1)));
    }

    private RateLimiterConfig config(String mode) {
        ServletContext context = (ServletContext) Proxy.newProxyInstance(ServletContext.class.getClassLoader(),
                new Class<?>[]{ServletContext.class}, (proxy, method, arguments) -> null);
        return RateLimiterConfig.from(context, Map.of(RateLimiterConfig.ENV_STORE_MODE, mode));
    }
}
