package hu.laci.cms.backend.config.app;

import javax.servlet.ServletContext;

/**
 * Utility methods for reading servlet context initialization parameters.
 * <p>
 * Configuration classes use this helper to keep null handling and simple type
 * parsing consistent while keeping domain-specific defaults in their own layer.
 */
public final class ServletContextParameters {

    private ServletContextParameters() {
    }

    public static String getString(ServletContext servletContext, String name) {
        return servletContext == null ? null : servletContext.getInitParameter(name);
    }

    public static int getInt(ServletContext servletContext, String name, int defaultValue) {
        String value = getString(servletContext, name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static boolean getBoolean(ServletContext servletContext, String name, boolean defaultValue) {
        String value = getString(servletContext, name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return Boolean.parseBoolean(value.trim());
    }
}
