package hu.laci.cms.backend.service.security;

import org.junit.jupiter.api.Test;

import javax.servlet.ServletContext;
import java.lang.reflect.Proxy;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RateLimiterConfigTest {

    @Test
    void defaultsToMemoryWhenEnvironmentAndWebXmlAreMissing() {
        RateLimiterConfig config = RateLimiterConfig.from(servletContext(Map.of()), Map.of());

        assertEquals(RateLimiterStoreMode.MEMORY, config.getStoreMode());
    }

    @Test
    void readsWebXmlWhenEnvironmentIsMissing() {
        RateLimiterConfig config = RateLimiterConfig.from(servletContext(Map.of(
                RateLimiterConfig.PARAM_STORE_MODE, "jdbc"
        )), Map.of());

        assertEquals(RateLimiterStoreMode.JDBC, config.getStoreMode());
    }

    @Test
    void environmentOverridesWebXml() {
        RateLimiterConfig config = RateLimiterConfig.from(servletContext(Map.of(
                RateLimiterConfig.PARAM_STORE_MODE, "memory"
        )), Map.of(RateLimiterConfig.ENV_STORE_MODE, "jdbc"));

        assertEquals(RateLimiterStoreMode.JDBC, config.getStoreMode());
    }

    @Test
    void explicitInvalidModeFails() {
        assertThrows(IllegalArgumentException.class,
                () -> RateLimiterConfig.from(servletContext(Map.of()), Map.of(
                        RateLimiterConfig.ENV_STORE_MODE, "invalid"
                )));
    }

    private ServletContext servletContext(Map<String, String> initParameters) {
        return (ServletContext) Proxy.newProxyInstance(
                ServletContext.class.getClassLoader(),
                new Class<?>[]{ServletContext.class},
                (proxy, method, args) -> {
                    if ("getInitParameter".equals(method.getName())) {
                        return initParameters.get((String) args[0]);
                    }
                    if ("toString".equals(method.getName())) {
                        return "TestServletContext";
                    }
                    return null;
                });
    }
}
