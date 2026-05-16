package hu.laci.cms.backend.servlet.filter;

import com.google.gson.Gson;
import hu.laci.cms.backend.dto.auth.AuthenticatedUser;
import hu.laci.cms.backend.dto.common.ApiErrorResponse;
import hu.laci.cms.backend.dto.common.ApiResponse;
import hu.laci.cms.backend.servlet.support.CsrfTokenSupport;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Set;

public class CsrfFilter implements Filter {

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private final Gson gson = new Gson();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (shouldSkip(httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = httpRequest.getSession(false);
        if (session == null || !(session.getAttribute("user") instanceof AuthenticatedUser)) {
            writeError(httpResponse, HttpServletResponse.SC_UNAUTHORIZED,
                    "AUTH_REQUIRED", "Authentication required");
            return;
        }

        Object sessionToken = session.getAttribute(CsrfTokenSupport.SESSION_ATTRIBUTE);
        String requestToken = httpRequest.getHeader(CsrfTokenSupport.HEADER_NAME);
        if (!(sessionToken instanceof String token) || token.isBlank() || !token.equals(requestToken)) {
            writeError(httpResponse, HttpServletResponse.SC_FORBIDDEN,
                    "CSRF_INVALID", "Invalid CSRF token");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean shouldSkip(HttpServletRequest request) {
        return SAFE_METHODS.contains(request.getMethod().toUpperCase())
                || LOGIN_PATH.equals(request.getServletPath());
    }

    private void writeError(HttpServletResponse response, int status, String code, String message)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(status);
        response.getWriter().write(gson.toJson(ApiResponse.error(new ApiErrorResponse(code, message))));
    }
}
