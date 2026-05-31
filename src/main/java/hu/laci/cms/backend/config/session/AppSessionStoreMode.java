package hu.laci.cms.backend.config.session;

/**
 * Supported backing stores for application session state.
 */
public enum AppSessionStoreMode {

    HTTP,
    JDBC,
    REDIS;

    /**
     * Parses a configured store mode.
     *
     * @param value configured value
     * @return parsed store mode
     * @throws IllegalArgumentException when the value is blank or unsupported
     */
    public static AppSessionStoreMode parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Session store mode must not be blank.");
        }

        return AppSessionStoreMode.valueOf(value.trim().toUpperCase());
    }
}
