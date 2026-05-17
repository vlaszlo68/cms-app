package hu.laci.cms.backend.servlet.filter;

import com.google.gson.Gson;
import hu.laci.cms.backend.dto.auth.AuthenticatedUser;
import hu.laci.cms.backend.dto.common.ApiErrorResponse;
import hu.laci.cms.backend.dto.common.ApiResponse;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Requires an authenticated HTTP session for protected API requests.
 * <p>
 * The filter expects the session attribute {@code user} to contain an
 * {@link AuthenticatedUser}. Login and logout endpoints are allowed through so
 * they can create or clear authentication state themselves.
 */
public class AuthFilter implements Filter {

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String LOGOUT_PATH = "/api/auth/logout";

    private final Gson gson = new Gson();

    /**
     * Checks authentication state and either continues the filter chain or writes {@code AUTH_REQUIRED}.
     *
     * @param request servlet request
     * @param response servlet response
     * @param chain downstream filter chain
     * @throws IOException when writing or downstream processing fails
     * @throws ServletException when downstream processing fails
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String servletPath = httpRequest.getServletPath();
        if (LOGIN_PATH.equals(servletPath) || LOGOUT_PATH.equals(servletPath)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = httpRequest.getSession(false);
        if (session == null || !(session.getAttribute("user") instanceof AuthenticatedUser)) {
            writeUnauthorized(httpResponse);
            return;
        }

        chain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write(gson.toJson(ApiResponse.error(
                new ApiErrorResponse("AUTH_REQUIRED", "Authentication required")
        )));
    }
}
