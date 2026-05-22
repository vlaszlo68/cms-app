package hu.laci.cms.backend.config.security;

import hu.laci.cms.backend.config.app.ServletContextParameters;

import javax.servlet.ServletContext;

/**
 * Central security configuration resolved from {@code web.xml} context parameters.
 * <p>
 * The class is initialized during application startup and also exposes safe
 * fallback defaults for tests or standalone service usage.
 */
public final class SecurityConfig {

    public static final int DEFAULT_MAX_FAILED_ATTEMPTS = 5;
    public static final int DEFAULT_LOCK_MINUTES = 15;

    private static volatile SecurityConfig current = defaults();

    private final PasswordPolicyConfig passwordPolicy;
    private final int maxFailedAttempts;
    private final int lockMinutes;
    private final boolean loginCaptchaEnabled;
    private final boolean registrationCaptchaEnabled;

    private SecurityConfig(PasswordPolicyConfig passwordPolicy, int maxFailedAttempts, int lockMinutes,
                           boolean loginCaptchaEnabled, boolean registrationCaptchaEnabled) {
        this.passwordPolicy = passwordPolicy;
        this.maxFailedAttempts = maxFailedAttempts;
        this.lockMinutes = lockMinutes;
        this.loginCaptchaEnabled = loginCaptchaEnabled;
        this.registrationCaptchaEnabled = registrationCaptchaEnabled;
    }

    public static void initialize(ServletContext servletContext) {
        current = from(servletContext);
    }

    public static void reset() {
        current = defaults();
    }

    public static SecurityConfig getCurrent() {
        return current;
    }

    public static SecurityConfig defaults() {
        return new SecurityConfig(PasswordPolicyConfig.defaults(), DEFAULT_MAX_FAILED_ATTEMPTS, DEFAULT_LOCK_MINUTES,
                true, true);
    }

    private static SecurityConfig from(ServletContext servletContext) {
        PasswordPolicyConfig passwordPolicy = new PasswordPolicyConfig(
                ServletContextParameters.getInt(servletContext, "password.min.length",
                        PasswordPolicyConfig.DEFAULT_MIN_LENGTH),
                ServletContextParameters.getBoolean(servletContext, "password.require.uppercase", true),
                ServletContextParameters.getBoolean(servletContext, "password.require.lowercase", true),
                ServletContextParameters.getBoolean(servletContext, "password.require.digit", true),
                ServletContextParameters.getBoolean(servletContext, "password.require.special", true)
        );

        return new SecurityConfig(
                passwordPolicy,
                ServletContextParameters.getInt(servletContext, "auth.max.failed.attempts",
                        DEFAULT_MAX_FAILED_ATTEMPTS),
                ServletContextParameters.getInt(servletContext, "auth.lock.minutes", DEFAULT_LOCK_MINUTES),
                ServletContextParameters.getBoolean(servletContext, "captcha.login.enabled", true),
                ServletContextParameters.getBoolean(servletContext, "captcha.registration.enabled", true)
        );
    }

    public PasswordPolicyConfig getPasswordPolicy() {
        return passwordPolicy;
    }

    public int getMaxFailedAttempts() {
        return maxFailedAttempts;
    }

    public int getLockMinutes() {
        return lockMinutes;
    }

    public boolean isLoginCaptchaEnabled() {
        return loginCaptchaEnabled;
    }

    public boolean isRegistrationCaptchaEnabled() {
        return registrationCaptchaEnabled;
    }
}
