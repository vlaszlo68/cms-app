package hu.laci.cms.backend.config.session;

import hu.laci.cms.backend.config.app.ServletContextParameters;

import javax.servlet.ServletContext;
import java.util.Map;

/**
 * Environment-first application session configuration.
 */
public final class AppSessionConfig {

    public static final String ENV_STORE_MODE = "SESSION_STORE_MODE";
    public static final String ENV_COOKIE_NAME = "SESSION_COOKIE_NAME";
    public static final String ENV_TIMEOUT_MINUTES = "SESSION_TIMEOUT_MINUTES";
    public static final String ENV_COOKIE_SECURE = "SESSION_COOKIE_SECURE";
    public static final String ENV_COOKIE_SAME_SITE = "SESSION_COOKIE_SAMESITE";

    public static final String PARAM_STORE_MODE = "session.store.mode";
    public static final String PARAM_COOKIE_NAME = "session.cookie.name";
    public static final String PARAM_TIMEOUT_MINUTES = "session.timeout.minutes";
    public static final String PARAM_COOKIE_SECURE = "session.cookie.secure";
    public static final String PARAM_COOKIE_SAME_SITE = "session.cookie.sameSite";

    private static final AppSessionConfig DEFAULT = new AppSessionConfig(
            AppSessionStoreMode.HTTP, "CMS_SESSION_ID", 30, false, "Lax");
    private static volatile AppSessionConfig current = DEFAULT;

    private final AppSessionStoreMode storeMode;
    private final String cookieName;
    private final int timeoutMinutes;
    private final boolean cookieSecure;
    private final String cookieSameSite;

    private AppSessionConfig(AppSessionStoreMode storeMode, String cookieName, int timeoutMinutes,
                             boolean cookieSecure, String cookieSameSite) {
        this.storeMode = storeMode;
        this.cookieName = cookieName;
        this.timeoutMinutes = timeoutMinutes;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
    }

    /**
     * Initializes the current configuration from environment variables first,
     * then servlet context parameters, then built-in defaults.
     *
     * @param servletContext servlet context for web.xml fallback values
     */
    public static void initialize(ServletContext servletContext) {
        current = from(servletContext);
    }

    public static void reset() {
        current = DEFAULT;
    }

    public static AppSessionConfig getCurrent() {
        return current;
    }

    private static AppSessionConfig from(ServletContext servletContext) {
        return from(servletContext, System.getenv());
    }

    static AppSessionConfig from(ServletContext servletContext, Map<String, String> environment) {
        String configuredMode = getEnvOrContext(environment, ENV_STORE_MODE, servletContext, PARAM_STORE_MODE);
        AppSessionStoreMode storeMode = configuredMode == null || configuredMode.isBlank()
                ? AppSessionStoreMode.HTTP
                : AppSessionStoreMode.parse(configuredMode);
        String cookieName = getEnvOrContext(environment, ENV_COOKIE_NAME, servletContext, PARAM_COOKIE_NAME);
        String timeoutMinutes = getEnvOrContext(environment, ENV_TIMEOUT_MINUTES, servletContext, PARAM_TIMEOUT_MINUTES);
        String cookieSecure = getEnvOrContext(environment, ENV_COOKIE_SECURE, servletContext, PARAM_COOKIE_SECURE);
        String cookieSameSite = getEnvOrContext(environment, ENV_COOKIE_SAME_SITE, servletContext, PARAM_COOKIE_SAME_SITE);

        return new AppSessionConfig(
                storeMode,
                isBlank(cookieName) ? DEFAULT.cookieName : cookieName.trim(),
                parsePositiveInt(timeoutMinutes, DEFAULT.timeoutMinutes),
                parseBoolean(cookieSecure, DEFAULT.cookieSecure),
                isBlank(cookieSameSite) ? DEFAULT.cookieSameSite : cookieSameSite.trim());
    }

    private static String getEnvOrContext(Map<String, String> environment, String envKey,
                                          ServletContext servletContext, String paramName) {
        String envValue = environment.get(envKey);
        if (!isBlank(envValue)) {
            return envValue;
        }
        return ServletContextParameters.getString(servletContext, paramName);
    }

    private static int parsePositiveInt(String value, int defaultValue) {
        if (isBlank(value)) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static boolean parseBoolean(String value, boolean defaultValue) {
        if (isBlank(value)) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public AppSessionStoreMode getStoreMode() {
        return storeMode;
    }

    public String getCookieName() {
        return cookieName;
    }

    public int getTimeoutMinutes() {
        return timeoutMinutes;
    }

    public boolean isCookieSecure() {
        return cookieSecure;
    }

    public String getCookieSameSite() {
        return cookieSameSite;
    }
}
