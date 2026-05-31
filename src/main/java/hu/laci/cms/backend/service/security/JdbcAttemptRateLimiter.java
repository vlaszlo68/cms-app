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
 * JDBC-backed failed-attempt lockout limiter.
 */
public class JdbcAttemptRateLimiter implements AttemptRateLimiter {

    private final String limiterName;
    private final int maxFailures;
    private final Duration lockDuration;

    /**
     * Creates a JDBC-backed attempt limiter.
     *
     * @param limiterName limiter namespace
     * @param maxFailures maximum failures before lockout
     * @param lockDuration lockout duration
     */
    public JdbcAttemptRateLimiter(String limiterName, int maxFailures, Duration lockDuration) {
        this.limiterName = Objects.requireNonNull(limiterName, "limiterName must not be null");
        this.maxFailures = Math.max(1, maxFailures);
        this.lockDuration = Objects.requireNonNull(lockDuration, "lockDuration must not be null");
    }

    @Override
    public boolean isLocked(String key) {
        try (Connection connection = DatabaseConfig.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT locked_until
                    FROM rate_limits
                    WHERE limiter_name = ? AND limiter_key = ?
                    """)) {
                statement.setString(1, limiterName);
                statement.setString(2, key);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return false;
                    }

                    Timestamp lockedUntil = resultSet.getTimestamp("locked_until");
                    if (lockedUntil == null) {
                        return false;
                    }
                    if (Instant.now().isBefore(lockedUntil.toInstant())) {
                        return true;
                    }
                }
            }

            recordSuccess(key);
            return false;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to check JDBC rate limit.", e);
        }
    }

    @Override
    public void recordFailure(String key) {
        try (Connection connection = DatabaseConfig.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                AttemptState state = loadForUpdate(connection, key);
                int failureCount = state == null ? 1 : state.failureCount() + 1;
                Instant lockedUntil = failureCount >= maxFailures ? Instant.now().plus(lockDuration) : null;
                upsert(connection, key, failureCount, lockedUntil);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to record JDBC rate limit failure.", e);
        }
    }

    @Override
    public void recordSuccess(String key) {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     DELETE FROM rate_limits
                     WHERE limiter_name = ? AND limiter_key = ?
                     """)) {
            statement.setString(1, limiterName);
            statement.setString(2, key);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to clear JDBC rate limit state.", e);
        }
    }

    private AttemptState loadForUpdate(Connection connection, String key) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT failure_count, locked_until
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
                Timestamp lockedUntil = resultSet.getTimestamp("locked_until");
                return new AttemptState(resultSet.getInt("failure_count"),
                        lockedUntil == null ? null : lockedUntil.toInstant());
            }
        }
    }

    private void upsert(Connection connection, String key, int failureCount, Instant lockedUntil)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO rate_limits (
                    limiter_name, limiter_key, failure_count, request_count,
                    window_started_at, locked_until, updated_at
                )
                VALUES (?, ?, ?, 0, NULL, ?, ?)
                ON CONFLICT (limiter_name, limiter_key)
                DO UPDATE SET
                    failure_count = EXCLUDED.failure_count,
                    locked_until = EXCLUDED.locked_until,
                    updated_at = EXCLUDED.updated_at
                """)) {
            statement.setString(1, limiterName);
            statement.setString(2, key);
            statement.setInt(3, failureCount);
            statement.setTimestamp(4, lockedUntil == null ? null : Timestamp.from(lockedUntil));
            statement.setTimestamp(5, Timestamp.from(Instant.now()));
            statement.executeUpdate();
        }
    }

    private record AttemptState(int failureCount, Instant lockedUntil) {
    }
}
