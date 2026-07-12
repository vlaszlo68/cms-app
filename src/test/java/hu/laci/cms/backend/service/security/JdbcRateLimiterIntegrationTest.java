package hu.laci.cms.backend.service.security;

import hu.laci.cms.backend.config.database.DatabaseConfig;
import hu.laci.cms.backend.config.database.migration.DatabaseMigrationRunner;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletContext;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;

/** Database-backed integration tests for shared JDBC attempt and request limiters. */
class JdbcRateLimiterIntegrationTest {

    private static final String PREFIX = "jdbc_rate_limiter_test_";

    @BeforeAll
    static void initializeDatabase() {
        DatabaseConfig.initialize(emptyServletContext());
        DatabaseMigrationRunner.runMigrations();
    }

    @AfterAll
    static void shutdownDatabase() {
        DatabaseConfig.shutdown();
    }

    @BeforeEach
    void setUp() throws SQLException {
        deleteTestRows();
    }

    @AfterEach
    void cleanUp() throws SQLException {
        deleteTestRows();
    }

    @Test
    void attemptLimiterLocksAfterThresholdAndRecordSuccessClearsState() {
        JdbcAttemptRateLimiter limiter = new JdbcAttemptRateLimiter(PREFIX + "attempt", 2, Duration.ofMinutes(1));

        limiter.recordFailure("client");
        Assertions.assertFalse(limiter.isLocked("client"));
        limiter.recordFailure("client");
        Assertions.assertTrue(limiter.isLocked("client"));
        limiter.recordSuccess("client");
        Assertions.assertFalse(limiter.isLocked("client"));
    }

    @Test
    void requestLimiterRejectsRequestsAboveWindowThreshold() {
        JdbcRequestRateLimiter limiter = new JdbcRequestRateLimiter(PREFIX + "request", 2, Duration.ofMinutes(1));

        Assertions.assertTrue(limiter.allowRequest("client"));
        Assertions.assertTrue(limiter.allowRequest("client"));
        Assertions.assertFalse(limiter.allowRequest("client"));
        Assertions.assertTrue(limiter.allowRequest("other-client"));
    }

    private void deleteTestRows() throws SQLException {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM rate_limits WHERE limiter_name LIKE ?")) {
            statement.setString(1, PREFIX + "%");
            statement.executeUpdate();
        }
    }

    private static ServletContext emptyServletContext() {
        return (ServletContext) Proxy.newProxyInstance(ServletContext.class.getClassLoader(),
                new Class<?>[]{ServletContext.class}, (proxy, method, arguments) -> {
                    if ("getInitParameter".equals(method.getName())) {
                        return null;
                    }
                    Class<?> returnType = method.getReturnType();
                    if (returnType == boolean.class) {
                        return false;
                    }
                    if (returnType == int.class) {
                        return 0;
                    }
                    return null;
                });
    }
}
