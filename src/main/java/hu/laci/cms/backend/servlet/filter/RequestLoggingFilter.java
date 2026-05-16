package hu.laci.cms.backend.servlet.filter;

import hu.laci.cms.backend.dto.auth.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

public class RequestLoggingFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        long startNanos = System.nanoTime();

        try {
            chain.doFilter(request, response);
            logCompletedRequest(httpRequest, httpResponse, startNanos);
        } catch (IOException | ServletException | RuntimeException | Error e) {
            logFailedRequest(httpRequest, httpResponse, startNanos, e);
            throw e;
        }
    }

    private void logCompletedRequest(HttpServletRequest request, HttpServletResponse response, long startNanos) {
        LOGGER.info("HTTP {} {} status={} durationMs={} remote={} user={}",
                request.getMethod(),
                getRequestTarget(request),
                response.getStatus(),
                getDurationMillis(startNanos),
                request.getRemoteAddr(),
                getUser(request));
    }

    private void logFailedRequest(HttpServletRequest request, HttpServletResponse response, long startNanos,
                                  Throwable throwable) {
        LOGGER.error("HTTP {} {} status={} durationMs={} remote={} user={} failed",
                request.getMethod(),
                getRequestTarget(request),
                response.getStatus(),
                getDurationMillis(startNanos),
                request.getRemoteAddr(),
                getUser(request),
                throwable);
    }

    private String getRequestTarget(HttpServletRequest request) {
        String queryString = request.getQueryString();
        if (queryString == null || queryString.isBlank()) {
            return request.getRequestURI();
        }

        return request.getRequestURI() + "?" + queryString;
    }

    private long getDurationMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private String getUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute("user") instanceof AuthenticatedUser user)) {
            return "-";
        }

        return user.getId() + ":" + user.getLoginName();
    }
}
