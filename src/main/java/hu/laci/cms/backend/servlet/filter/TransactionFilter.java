package hu.laci.cms.backend.servlet.filter;

import hu.laci.cms.backend.config.database.TransactionContext;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Wraps request processing in a request-scoped database transaction.
 * <p>
 * It initializes {@link TransactionContext}, lets downstream filters and
 * servlets run, then commits on success or rolls back on exceptions and
 * rollback-only state.
 */
public class TransactionFilter implements Filter {

    /**
     * Runs downstream request processing inside a transaction boundary.
     *
     * @param request servlet request
     * @param response servlet response
     * @param chain downstream filter chain
     * @throws IOException when downstream processing fails
     * @throws ServletException when transaction handling or downstream processing fails
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            TransactionContext.begin();
            chain.doFilter(request, response);
            commitOrRollback();
        } catch (IOException | ServletException | RuntimeException | Error e) {
            rollback();
            throw e;
        } catch (SQLException e) {
            rollback();
            throw new ServletException("Failed to handle request transaction.", e);
        } finally {
            close();
        }
    }

    private void commitOrRollback() throws ServletException {
        try {
            if (TransactionContext.isRollbackOnly()) {
                TransactionContext.rollback();
                return;
            }

            TransactionContext.commit();
        } catch (SQLException e) {
            throw new ServletException("Failed to finish request transaction.", e);
        }
    }

    private void rollback() throws ServletException {
        try {
            TransactionContext.rollback();
        } catch (SQLException e) {
            throw new ServletException("Failed to rollback request transaction.", e);
        }
    }

    private void close() throws ServletException {
        try {
            TransactionContext.close();
        } catch (SQLException e) {
            throw new ServletException("Failed to close request transaction.", e);
        }
    }
}
