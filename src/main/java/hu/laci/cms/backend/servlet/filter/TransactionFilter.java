package hu.laci.cms.backend.servlet.filter;

import hu.laci.cms.backend.config.database.TransactionContext;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.sql.SQLException;

public class TransactionFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            TransactionContext.begin();
            chain.doFilter(request, response);
            TransactionContext.commit();
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
