package hu.laci.cms.backend.config.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Thread-local transaction holder used by the JDBC DAO layer.
 * <p>
 * {@link hu.laci.cms.backend.servlet.filter.TransactionFilter} starts a
 * request-scoped transaction by calling {@link #begin()}, then commits,
 * rolls back, and closes it at the end of request processing. DAO code should
 * obtain connections through {@link #openConnection()} so it automatically
 * participates in the active request transaction when one exists.
 * <p>
 * Outside an active transaction, {@link #openConnection()} returns an
 * independent connection scope that closes the connection when the scope is
 * closed. This keeps DAO tests and non-request code usable without manually
 * starting a transaction.
 */
public final class TransactionContext {

    private static final ThreadLocal<Connection> CURRENT_CONNECTION = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> ROLLBACK_ONLY = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private TransactionContext() {
    }

    /**
     * Starts a transaction on the current thread.
     * <p>
     * A connection is acquired from {@link DatabaseConfig}, auto-commit is
     * disabled, and the connection is stored in a thread-local holder. Calling
     * this method while another transaction is already active on the same thread
     * is a programming error.
     *
     * @throws SQLException when connection acquisition or transaction setup fails
     * @throws IllegalStateException when a transaction is already active on the current thread
     */
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

    /**
     * Returns the currently active transaction connection.
     *
     * @return active thread-local connection, or empty when no transaction is active
     */
    public static Optional<Connection> getCurrentConnection() {
        return Optional.ofNullable(CURRENT_CONNECTION.get());
    }

    /**
     * Opens a connection scope for DAO code.
     * <p>
     * When a request transaction is active, the returned scope wraps the current
     * transaction connection and closing the scope does not close the underlying
     * connection. When no transaction is active, a new standalone connection is
     * opened and closing the scope closes that connection.
     *
     * <pre>{@code
     * try (TransactionContext.ConnectionScope scope = TransactionContext.openConnection();
     *      PreparedStatement statement = scope.getConnection().prepareStatement(sql)) {
     *     // bind parameters and execute SQL
     * }
     * }</pre>
     *
     * @return connection scope suitable for try-with-resources
     * @throws SQLException when opening a standalone connection fails
     */
    public static ConnectionScope openConnection() throws SQLException {
        Connection currentConnection = CURRENT_CONNECTION.get();
        if (currentConnection != null) {
            return new ConnectionScope(currentConnection, false);
        }

        return new ConnectionScope(DatabaseConfig.getConnection(), true);
    }

    /**
     * Commits the active transaction unless it has been marked rollback-only.
     * <p>
     * If {@link #setRollbackOnly()} was called, this method rolls back instead
     * of committing. Calling it without an active transaction is a no-op.
     *
     * @throws SQLException when commit or rollback fails
     */
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

    /**
     * Rolls back the active transaction.
     * <p>
     * Calling it without an active transaction is a no-op.
     *
     * @throws SQLException when rollback fails
     */
    public static void rollback() throws SQLException {
        Connection connection = CURRENT_CONNECTION.get();
        if (connection != null) {
            connection.rollback();
        }
    }

    /**
     * Marks the active transaction so it must roll back at request end.
     * <p>
     * This is useful when application code catches an exception and returns a
     * handled response, but the database changes made earlier in the request
     * still must not be committed.
     *
     * @throws IllegalStateException when no transaction is active on the current thread
     */
    public static void setRollbackOnly() {
        if (CURRENT_CONNECTION.get() == null) {
            throw new IllegalStateException("No transaction is active on the current thread.");
        }

        ROLLBACK_ONLY.set(Boolean.TRUE);
    }

    /**
     * Returns whether the current transaction has been marked rollback-only.
     *
     * @return {@code true} when request end should roll back instead of commit
     */
    public static boolean isRollbackOnly() {
        return Boolean.TRUE.equals(ROLLBACK_ONLY.get());
    }

    /**
     * Closes and clears the current thread-local transaction state.
     * <p>
     * The connection auto-commit flag is restored before closing. This method is
     * normally called by {@code TransactionFilter} in a {@code finally} block.
     * Calling it without an active transaction only clears thread-local state.
     *
     * @throws SQLException when restoring auto-commit or closing the connection fails
     */
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

    /**
     * Auto-closeable connection wrapper returned by {@link #openConnection()}.
     * <p>
     * The scope knows whether it owns the underlying connection. Request-bound
     * scopes do not close the transaction connection; standalone scopes close
     * their connection on {@link #close()}.
     */
    public static final class ConnectionScope implements AutoCloseable {

        private final Connection connection;
        private final boolean closeConnection;

        private ConnectionScope(Connection connection, boolean closeConnection) {
            this.connection = connection;
            this.closeConnection = closeConnection;
        }

        /**
         * Returns the JDBC connection for SQL work.
         *
         * @return active or standalone connection
         */
        public Connection getConnection() {
            return connection;
        }

        /**
         * Closes the scope.
         * <p>
         * The underlying connection is closed only when the scope opened a
         * standalone connection outside an active transaction.
         *
         * @throws SQLException when closing the owned connection fails
         */
        @Override
        public void close() throws SQLException {
            if (closeConnection) {
                connection.close();
            }
        }
    }
}
