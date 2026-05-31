package hu.laci.cms.backend.config.session;

import org.junit.jupiter.api.Test;

import javax.servlet.ServletContext;
import java.lang.reflect.Proxy;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AppSessionConfigTest {

    @Test
    void defaultsToHttpWhenEnvironmentAndWebXmlAreMissing() {
        AppSessionConfig config = AppSessionConfig.from(servletContext(Map.of()), Map.of());

        assertEquals(AppSessionStoreMode.HTTP, config.getStoreMode());
        assertEquals("CMS_SESSION_ID", config.getCookieName());
        assertEquals(30, config.getTimeoutMinutes());
    }

    @Test
    void readsWebXmlWhenEnvironmentIsMissing() {
        AppSessionConfig config = AppSessionConfig.from(servletContext(Map.of(
                AppSessionConfig.PARAM_STORE_MODE, "jdbc",
                AppSessionConfig.PARAM_COOKIE_NAME, "CUSTOM_SESSION",
                AppSessionConfig.PARAM_TIMEOUT_MINUTES, "45"
        )), Map.of());

        assertEquals(AppSessionStoreMode.JDBC, config.getStoreMode());
        assertEquals("CUSTOM_SESSION", config.getCookieName());
        assertEquals(45, config.getTimeoutMinutes());
    }

    @Test
    void environmentOverridesWebXml() {
        AppSessionConfig config = AppSessionConfig.from(servletContext(Map.of(
                AppSessionConfig.PARAM_STORE_MODE, "http"
        )), Map.of(
                AppSessionConfig.ENV_STORE_MODE, "jdbc",
                AppSessionConfig.ENV_TIMEOUT_MINUTES, "60"
        ));

        assertEquals(AppSessionStoreMode.JDBC, config.getStoreMode());
        assertEquals(60, config.getTimeoutMinutes());
    }

    @Test
    void explicitInvalidModeFails() {
        assertThrows(IllegalArgumentException.class,
                () -> AppSessionConfig.from(servletContext(Map.of()), Map.of(
                        AppSessionConfig.ENV_STORE_MODE, "invalid"
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
