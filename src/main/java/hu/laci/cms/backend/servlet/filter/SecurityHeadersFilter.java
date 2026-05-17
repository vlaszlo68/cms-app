package hu.laci.cms.backend.servlet.filter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Adds cache-control and browser security headers to every response.
 * <p>
 * The current header set disables caching and adds basic hardening headers such
 * as {@code X-Content-Type-Options}, {@code X-Frame-Options},
 * {@code Referrer-Policy}, and {@code Permissions-Policy}.
 */
public class SecurityHeadersFilter implements Filter {

    /**
     * Adds security headers before continuing the filter chain.
     *
     * @param request servlet request
     * @param response servlet response
     * @param chain downstream filter chain
     * @throws IOException when downstream processing fails
     * @throws ServletException when downstream processing fails
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        setSecurityHeaders(httpResponse);
        chain.doFilter(request, response);
    }

    private void setSecurityHeaders(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
    }
}
