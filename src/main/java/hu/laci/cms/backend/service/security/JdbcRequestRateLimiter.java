package hu.laci.cms.backend.service.security;

import hu.laci.cms.backend.config.database.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * JDBC-backed fixed-window request limiter.
 */
public class JdbcRequestRateLimiter implements RequestRateLimiter {

    private final String limiterName;
    private final int maxRequests;
    private final Duration windowDuration;

    /**
     * Creates a JDBC-backed request limiter.
     *
     * @param limiterName limiter namespace
     * @param maxRequests maximum requests per window
     * @param windowDuration window duration
     */
    public JdbcRequestRateLimiter(String limiterName, int maxRequests, Duration windowDuration) {
        this.limiterName = Objects.requireNonNull(limiterName, "limiterName must not be null");
        this.maxRequests = Math.max(1, maxRequests);
        this.windowDuration = Objects.requireNonNull(windowDuration, "windowDuration must not be null");
    }

    @Override
    public boolean allowRequest(String key) {
        try (Connection connection = DatabaseConfig.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                Instant now = Instant.now();
                WindowState state = loadForUpdate(connection, key);
                WindowState updatedState = nextState(state, now);
                upsert(connection, key, updatedState);
                connection.commit();
                return updatedState.requestCount() <= maxRequests;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to record JDBC request rate limit.", e);
        }
    }

    private WindowState nextState(WindowState currentState, Instant now) {
        if (currentState == null || !now.isBefore(currentState.windowStartedAt().plus(windowDuration))) {
            return new WindowState(now, 1);
        }
        return new WindowState(currentState.windowStartedAt(), currentState.requestCount() + 1);
    }

    private WindowState loadForUpdate(Connection connection, String key) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT request_count, window_started_at
                FROM rate_limits
                WHERE limiter_name = ? AND limiter_key = ?
                FOR UPDATE
                """)) {
            statement.setString(1, limiterName);
            statement.setString(2, key);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                Timestamp windowStartedAt = resultSet.getTimestamp("window_started_at");
                if (windowStartedAt == null) {
                    return null;
                }
                return new WindowState(windowStartedAt.toInstant(), resultSet.getInt("request_count"));
            }
        }
    }

    private void upsert(Connection connection, String key, WindowState state) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO rate_limits (
                    limiter_name, limiter_key, failure_count, request_count,
                    window_started_at, locked_until, updated_at
                )
                VALUES (?, ?, 0, ?, ?, NULL, ?)
                ON CONFLICT (limiter_name, limiter_key)
                DO UPDATE SET
                    request_count = EXCLUDED.request_count,
                    window_started_at = EXCLUDED.window_started_at,
                    updated_at = EXCLUDED.updated_at
                """)) {
            statement.setString(1, limiterName);
            statement.setString(2, key);
            statement.setInt(3, state.requestCount());
            statement.setTimestamp(4, Timestamp.from(state.windowStartedAt()));
            statement.setTimestamp(5, Timestamp.from(Instant.now()));
            statement.executeUpdate();
        }
    }

    private record WindowState(Instant windowStartedAt, int requestCount) {
    }
}
