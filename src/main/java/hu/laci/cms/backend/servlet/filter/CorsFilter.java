package hu.laci.cms.backend.servlet.filter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class CorsFilter implements Filter {

    private static final String ALLOWED_ORIGINS_PARAM = "allowedOrigins";
    private static final String DEFAULT_ALLOWED_METHODS = "GET, POST, PUT, DELETE, OPTIONS";
    private static final String DEFAULT_ALLOWED_HEADERS = "Content-Type, Authorization, X-CSRF-Token";
    private static final String MAX_AGE_SECONDS = "3600";

    private Set<String> allowedOrigins = Set.of();

    @Override
    public void init(FilterConfig filterConfig) {
        String configuredOrigins = filterConfig.getInitParameter(ALLOWED_ORIGINS_PARAM);
        if (configuredOrigins == null || configuredOrigins.isBlank()) {
            allowedOrigins = Set.of();
            return;
        }

        allowedOrigins = Arrays.stream(configuredOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String origin = httpRequest.getHeader("Origin");
        boolean allowedOrigin = origin != null && allowedOrigins.contains(origin);
        if (allowedOrigin) {
            setCorsHeaders(httpResponse, origin);
        }

        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            httpResponse.setStatus(allowedOrigin
                    ? HttpServletResponse.SC_NO_CONTENT
                    : HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        chain.doFilter(request, response);
    }

    private void setCorsHeaders(HttpServletResponse response, String origin) {
        response.setHeader("Access-Control-Allow-Origin", origin);
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Methods", DEFAULT_ALLOWED_METHODS);
        response.setHeader("Access-Control-Allow-Headers", DEFAULT_ALLOWED_HEADERS);
        response.setHeader("Access-Control-Max-Age", MAX_AGE_SECONDS);
        response.setHeader("Vary", "Origin");
    }
}
