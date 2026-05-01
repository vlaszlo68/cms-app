package hu.laci.cms.backend.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.servlet.ServletContext;
import java.sql.Connection;
import java.sql.SQLException;

public final class DatabaseConfig {

    private static final String DEFAULT_DB_HOST = "localhost";
    private static final String DEFAULT_DB_PORT = "5432";
    private static final String DEFAULT_DB_NAME = "cms_db";
    private static final String DEFAULT_DB_USER = "cms_user";
    private static final String DEFAULT_DB_PASSWORD = "cms_pw";

    private static volatile HikariDataSource dataSource;

    private DatabaseConfig() {
    }

    public static void initialize(ServletContext servletContext) {
        if (dataSource != null) {
            return;
        }

        synchronized (DatabaseConfig.class) {
            if (dataSource != null) {
                return;
            }

            String xmlJdbcUrl = getInitParameter(servletContext, "db.jdbcUrl");
            String host = getEnvOrFallback("DB_HOST", extractJdbcUrlPart(xmlJdbcUrl, JdbcUrlPart.HOST), DEFAULT_DB_HOST);
            String port = getEnvOrFallback("DB_PORT", extractJdbcUrlPart(xmlJdbcUrl, JdbcUrlPart.PORT), DEFAULT_DB_PORT);
            String databaseName = getEnvOrFallback("DB_NAME", extractJdbcUrlPart(xmlJdbcUrl, JdbcUrlPart.DATABASE), DEFAULT_DB_NAME);
            String username = getEnvOrFallback("DB_USER", getInitParameter(servletContext, "db.username"), DEFAULT_DB_USER);
            String password = getEnvOrFallback("DB_PASSWORD", getInitParameter(servletContext, "db.password"), DEFAULT_DB_PASSWORD);

            HikariConfig config = new HikariConfig();
            config.setDriverClassName("org.postgresql.Driver");
            config.setJdbcUrl(buildJdbcUrl(host, port, databaseName));
            config.setUsername(username);
            config.setPassword(password);

            dataSource = new HikariDataSource(config);
        }
    }

    private static String getEnvOrFallback(String envKey, String xmlValue, String defaultValue) {
        String envValue = System.getenv(envKey);
        if (!isBlank(envValue)) {
            return envValue.trim();
        }

        if (!isBlank(xmlValue)) {
            return xmlValue.trim();
        }

        return defaultValue;
    }

    private static String getInitParameter(ServletContext servletContext, String name) {
        return servletContext.getInitParameter(name);
    }

    private static String buildJdbcUrl(String host, String port, String databaseName) {
        return "jdbc:postgresql://" + host + ":" + port + "/" + databaseName;
    }

    private static String extractJdbcUrlPart(String jdbcUrl, JdbcUrlPart part) {
        if (isBlank(jdbcUrl)) {
            return null;
        }

        String trimmedUrl = jdbcUrl.trim();
        String prefix = "jdbc:postgresql://";
        if (!trimmedUrl.startsWith(prefix)) {
            return null;
        }

        String remainder = trimmedUrl.substring(prefix.length());
        int slashIndex = remainder.indexOf('/');
        if (slashIndex < 0) {
            return null;
        }

        String hostAndPort = remainder.substring(0, slashIndex);
        String databaseName = remainder.substring(slashIndex + 1);
        int colonIndex = hostAndPort.lastIndexOf(':');

        String host = colonIndex >= 0 ? hostAndPort.substring(0, colonIndex) : hostAndPort;
        String port = colonIndex >= 0 ? hostAndPort.substring(colonIndex + 1) : null;

        if (part == JdbcUrlPart.HOST) {
            return host;
        }

        if (part == JdbcUrlPart.PORT) {
            return port;
        }

        return databaseName;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new IllegalStateException("DatabaseConfig is not initialized.");
        }

        return dataSource.getConnection();
    }

    public static void shutdown() {
        HikariDataSource currentDataSource = dataSource;
        if (currentDataSource != null) {
            currentDataSource.close();
            dataSource = null;
        }
    }

    private enum JdbcUrlPart {
        HOST,
        PORT,
        DATABASE
    }
}
