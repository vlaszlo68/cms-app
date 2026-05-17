package hu.laci.cms.backend.servlet.filter;

import com.google.gson.Gson;
import hu.laci.cms.backend.dto.common.ApiErrorResponse;
import hu.laci.cms.backend.dto.common.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Converts unhandled request exceptions into the common JSON error envelope.
 * <p>
 * If the response has not been committed yet, the filter writes a
 * {@code 500 INTERNAL_ERROR} response and logs the original exception. If the
 * response is already committed, the original exception is rethrown.
 */
public class ExceptionHandlingFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionHandlingFilter.class);
    private static final String INTERNAL_ERROR_CODE = "INTERNAL_ERROR";
    private static final String INTERNAL_ERROR_MESSAGE = "Internal server error.";

    private final Gson gson = new Gson();

    /**
     * Executes downstream processing and converts unhandled exceptions to JSON when possible.
     *
     * @param request servlet request
     * @param response servlet response
     * @param chain downstream filter chain
     * @throws IOException when downstream processing or response writing fails
     * @throws ServletException when downstream processing fails
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException e) {
            handleException(response, e);
        }
    }

    private void handleException(ServletResponse response, Exception exception)
            throws IOException, ServletException {
        if (!(response instanceof HttpServletResponse httpResponse)) {
            throwAsServletException(exception);
            return;
        }

        if (httpResponse.isCommitted()) {
            throwAsServletException(exception);
            return;
        }

        LOGGER.error("Unhandled request exception.", exception);
        httpResponse.resetBuffer();
        httpResponse.setContentType("application/json");
        httpResponse.setCharacterEncoding("UTF-8");
        httpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        httpResponse.getWriter().write(gson.toJson(ApiResponse.error(
                new ApiErrorResponse(INTERNAL_ERROR_CODE, INTERNAL_ERROR_MESSAGE)
        )));
    }

    private void throwAsServletException(Exception exception) throws IOException, ServletException {
        if (exception instanceof IOException ioException) {
            throw ioException;
        }
        if (exception instanceof ServletException servletException) {
            throw servletException;
        }

        throw new ServletException(exception);
    }
}
