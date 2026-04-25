package hu.laci.cms.backend.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.servlet.ServletContext;
import java.sql.Connection;
import java.sql.SQLException;

public final class DatabaseConfig {

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

            HikariConfig config = new HikariConfig();
            config.setDriverClassName("org.postgresql.Driver");
            config.setJdbcUrl(getRequiredInitParameter(servletContext, "db.jdbcUrl"));
            config.setUsername(getRequiredInitParameter(servletContext, "db.username"));
            config.setPassword(getRequiredInitParameter(servletContext, "db.password"));

            dataSource = new HikariDataSource(config);
        }
    }

    private static String getRequiredInitParameter(ServletContext servletContext, String name) {
        String value = servletContext.getInitParameter(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required web.xml context-param: " + name);
        }

        return value;
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
}
