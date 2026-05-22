package hu.laci.cms.backend.config.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletContext;
import java.lang.reflect.Proxy;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigTest {

    @AfterEach
    void tearDown() {
        SecurityConfig.reset();
    }

    @Test
    void captchaFlagsDefaultToEnabledWhenContextParamsAreMissing() {
        SecurityConfig.initialize(createServletContext(Map.of()));

        SecurityConfig securityConfig = SecurityConfig.getCurrent();
        assertTrue(securityConfig.isLoginCaptchaEnabled());
        assertTrue(securityConfig.isRegistrationCaptchaEnabled());
    }

    @Test
    void captchaFlagsCanBeDisabledFromContextParams() {
        SecurityConfig.initialize(createServletContext(Map.of(
                "captcha.login.enabled", "false",
                "captcha.registration.enabled", "false"
        )));

        SecurityConfig securityConfig = SecurityConfig.getCurrent();
        assertFalse(securityConfig.isLoginCaptchaEnabled());
        assertFalse(securityConfig.isRegistrationCaptchaEnabled());
    }

    private static ServletContext createServletContext(Map<String, String> initParameters) {
        return (ServletContext) Proxy.newProxyInstance(
                SecurityConfigTest.class.getClassLoader(),
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
