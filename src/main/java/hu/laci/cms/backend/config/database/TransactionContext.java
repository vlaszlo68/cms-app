package hu.laci.cms.backend.config.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

public final class TransactionContext {

    private static final ThreadLocal<Connection> CURRENT_CONNECTION = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> ROLLBACK_ONLY = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private TransactionContext() {
    }

    public static void begin() throws SQLException {
        if (CURRENT_CONNECTION.get() != null) {
            throw new IllegalStateException("Transaction is already active on the current thread.");
        }

        Connection connection = DatabaseConfig.getConnection();
        try {
            connection.setAutoCommit(false);
            ROLLBACK_ONLY.set(Boolean.FALSE);
            CURRENT_CONNECTION.set(connection);
        } catch (SQLException | RuntimeException e) {
            connection.close();
            throw e;
        }
    }

    public static Optional<Connection> getCurrentConnection() {
        return Optional.ofNullable(CURRENT_CONNECTION.get());
    }

    public static ConnectionScope openConnection() throws SQLException {
        Connection currentConnection = CURRENT_CONNECTION.get();
        if (currentConnection != null) {
            return new ConnectionScope(currentConnection, false);
        }

        return new ConnectionScope(DatabaseConfig.getConnection(), true);
    }

    public static void commit() throws SQLException {
        Connection connection = CURRENT_CONNECTION.get();
        if (connection != null) {
            if (isRollbackOnly()) {
                connection.rollback();
                return;
            }

            connection.commit();
        }
    }

    public static void rollback() throws SQLException {
        Connection connection = CURRENT_CONNECTION.get();
        if (connection != null) {
            connection.rollback();
        }
    }

    public static void setRollbackOnly() {
        if (CURRENT_CONNECTION.get() == null) {
            throw new IllegalStateException("No transaction is active on the current thread.");
        }

        ROLLBACK_ONLY.set(Boolean.TRUE);
    }

    public static boolean isRollbackOnly() {
        return Boolean.TRUE.equals(ROLLBACK_ONLY.get());
    }

    public static void close() throws SQLException {
        Connection connection = CURRENT_CONNECTION.get();
        CURRENT_CONNECTION.remove();
        ROLLBACK_ONLY.remove();

        if (connection != null) {
            try {
                connection.setAutoCommit(true);
            } finally {
                connection.close();
            }
        }
    }

    public static final class ConnectionScope implements AutoCloseable {

        private final Connection connection;
        private final boolean closeConnection;

        private ConnectionScope(Connection connection, boolean closeConnection) {
            this.connection = connection;
            this.closeConnection = closeConnection;
        }

        public Connection getConnection() {
            return connection;
        }

        @Override
        public void close() throws SQLException {
            if (closeConnection) {
                connection.close();
            }
        }
    }
}
