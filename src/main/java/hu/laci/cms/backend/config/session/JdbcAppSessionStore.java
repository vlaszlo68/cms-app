package hu.laci.cms.backend.config.session;

import hu.laci.cms.backend.config.database.DatabaseConfig;
import hu.laci.cms.backend.dto.auth.AuthenticatedUser;
import hu.laci.cms.backend.model.user.UserRole;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Application session store backed by PostgreSQL tables.
 */
public class JdbcAppSessionStore implements AppSessionStore {

    private final AppSessionConfig config;
    private final SessionCookieSupport cookieSupport;

    /**
     * Creates a JDBC-backed application session store.
     *
     * @param config session configuration
     */
    public JdbcAppSessionStore(AppSessionConfig config) {
        this.config = config;
        this.cookieSupport = new SessionCookieSupport(config);
    }

    @Override
    public Optional<AppSession> find(HttpServletRequest request, HttpServletResponse response) {
        Optional<String> sessionId = cookieSupport.readSessionId(request);
        if (sessionId.isEmpty()) {
            return Optional.empty();
        }

        String idHash = hash(sessionId.get());
        try (Connection connection = DatabaseConfig.getConnection()) {
            Optional<AppSession> session = findByHash(connection, sessionId.get(), idHash);
            if (session.isEmpty()) {
                if (response != null) {
                    cookieSupport.clearSessionCookie(response);
                }
                return Optional.empty();
            }

            AppSession appSession = session.get();
            Instant now = Instant.now();
            if (appSession.isInvalidated() || appSession.isExpired(now)) {
                deleteSession(connection, idHash);
                if (response != null) {
                    cookieSupport.clearSessionCookie(response);
                }
                return Optional.empty();
            }

            appSession.setLastAccessedAt(now);
            appSession.setExpiresAt(now.plus(Duration.ofMinutes(config.getTimeoutMinutes())));
            save(request, response, appSession);
            return Optional.of(appSession);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load JDBC-backed session.", e);
        }
    }

    @Override
    public AppSession create(HttpServletRequest request, HttpServletResponse response) {
        String sessionId = SessionIdGenerator.generate();
        Instant now = Instant.now();
        AppSession session = new AppSession(sessionId, null, null, now, now,
                now.plus(Duration.ofMinutes(config.getTimeoutMinutes())));
        cookieSupport.addSessionCookie(response, sessionId);
        save(request, response, session);
        return session;
    }

    @Override
    public void save(HttpServletRequest request, HttpServletResponse response, AppSession session) {
        String idHash = hash(session.getId());
        try (Connection connection = DatabaseConfig.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                upsertSession(connection, idHash, session);
                saveAttributes(connection, idHash, session);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save JDBC-backed session.", e);
        }
    }

    @Override
    public void invalidate(HttpServletRequest request, HttpServletResponse response) {
        Optional<String> sessionId = cookieSupport.readSessionId(request);
        if (sessionId.isPresent()) {
            try (Connection connection = DatabaseConfig.getConnection()) {
                deleteSession(connection, hash(sessionId.get()));
            } catch (SQLException e) {
                throw new IllegalStateException("Failed to invalidate JDBC-backed session.", e);
            }
        }
        if (response != null) {
            cookieSupport.clearSessionCookie(response);
        }
    }

    private Optional<AppSession> findByHash(Connection connection, String sessionId, String idHash)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT user_id, login_name, email, role, csrf_token, created_at,
                       last_accessed_at, expires_at, invalidated
                FROM app_sessions
                WHERE id_hash = ?
                """)) {
            statement.setString(1, idHash);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                AuthenticatedUser authenticatedUser = toAuthenticatedUser(resultSet);
                AppSession session = new AppSession(
                        sessionId,
                        authenticatedUser,
                        resultSet.getString("csrf_token"),
                        resultSet.getTimestamp("created_at").toInstant(),
                        resultSet.getTimestamp("last_accessed_at").toInstant(),
                        resultSet.getTimestamp("expires_at").toInstant());
                session.setInvalidated("T".equals(resultSet.getString("invalidated")));
                loadAttributes(connection, idHash, session);
                return Optional.of(session);
            }
        }
    }

    private AuthenticatedUser toAuthenticatedUser(ResultSet resultSet) throws SQLException {
        long userId = resultSet.getLong("user_id");
        if (resultSet.wasNull()) {
            return null;
        }

        String role = resultSet.getString("role");
        return new AuthenticatedUser(
                userId,
                resultSet.getString("login_name"),
                resultSet.getString("email"),
                role == null ? null : UserRole.valueOf(role));
    }

    private void loadAttributes(Connection connection, String idHash, AppSession session) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT attribute_name, attribute_type, json_value, created_at, updated_at
                FROM app_session_attributes
                WHERE session_id_hash = ?
                """)) {
            statement.setString(1, idHash);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    session.putAttribute(new AppSessionAttribute(
                            resultSet.getString("attribute_name"),
                            AppSessionAttributeType.valueOf(resultSet.getString("attribute_type")),
                            resultSet.getString("json_value"),
                            resultSet.getTimestamp("created_at").toInstant(),
                            resultSet.getTimestamp("updated_at").toInstant()));
                }
            }
        }
    }

    private void upsertSession(Connection connection, String idHash, AppSession session) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO app_sessions (
                    id_hash, user_id, login_name, email, role, csrf_token,
                    created_at, last_accessed_at, expires_at, invalidated
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id_hash)
                DO UPDATE SET
                    user_id = EXCLUDED.user_id,
                    login_name = EXCLUDED.login_name,
                    email = EXCLUDED.email,
                    role = EXCLUDED.role,
                    csrf_token = EXCLUDED.csrf_token,
                    last_accessed_at = EXCLUDED.last_accessed_at,
                    expires_at = EXCLUDED.expires_at,
                    invalidated = EXCLUDED.invalidated
                """)) {
            statement.setString(1, idHash);
            Optional<AuthenticatedUser> authenticatedUser = session.getAuthenticatedUser();
            if (authenticatedUser.isPresent()) {
                AuthenticatedUser user = authenticatedUser.get();
                statement.setLong(2, user.getId());
                statement.setString(3, user.getLoginName());
                statement.setString(4, user.getEmail());
                statement.setString(5, user.getRole() == null ? null : user.getRole().name());
            } else {
                statement.setObject(2, null);
                statement.setString(3, null);
                statement.setString(4, null);
                statement.setString(5, null);
            }
            statement.setString(6, session.getCsrfToken());
            statement.setTimestamp(7, Timestamp.from(session.getCreatedAt()));
            statement.setTimestamp(8, Timestamp.from(session.getLastAccessedAt()));
            statement.setTimestamp(9, Timestamp.from(session.getExpiresAt()));
            statement.setString(10, session.isInvalidated() ? "T" : "F");
            statement.executeUpdate();
        }
    }

    private void saveAttributes(Connection connection, String idHash, AppSession session) throws SQLException {
        for (AppSessionAttribute attribute : session.getAttributes().values()) {
            if (attribute.isDeleted()) {
                deleteAttribute(connection, idHash, attribute.getName());
            } else {
                upsertAttribute(connection, idHash, attribute);
            }
        }
    }

    private void upsertAttribute(Connection connection, String idHash, AppSessionAttribute attribute)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO app_session_attributes (
                    session_id_hash, attribute_name, attribute_type, json_value, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (session_id_hash, attribute_name)
                DO UPDATE SET
                    attribute_type = EXCLUDED.attribute_type,
                    json_value = EXCLUDED.json_value,
                    updated_at = EXCLUDED.updated_at
                """)) {
            statement.setString(1, idHash);
            statement.setString(2, attribute.getName());
            statement.setString(3, attribute.getType().name());
            statement.setString(4, attribute.getJsonValue());
            statement.setTimestamp(5, Timestamp.from(attribute.getCreatedAt()));
            statement.setTimestamp(6, Timestamp.from(attribute.getUpdatedAt()));
            statement.executeUpdate();
        }
    }

    private void deleteAttribute(Connection connection, String idHash, String attributeName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                DELETE FROM app_session_attributes
                WHERE session_id_hash = ? AND attribute_name = ?
                """)) {
            statement.setString(1, idHash);
            statement.setString(2, attributeName);
            statement.executeUpdate();
        }
    }

    private void deleteSession(Connection connection, String idHash) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM app_sessions WHERE id_hash = ?")) {
            statement.setString(1, idHash);
            statement.executeUpdate();
        }
    }

    private static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available.", e);
        }
    }
}
