package hu.laci.cms.backend.servlet.auth;

import hu.laci.cms.backend.dto.auth.AuthenticatedUser;
import hu.laci.cms.backend.dto.auth.AuthUserResponse;
import hu.laci.cms.backend.servlet.support.CsrfTokenSupport;
import hu.laci.cms.backend.servlet.support.JsonServletSupport;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Session restore endpoint for the currently authenticated user.
 * <p>
 * Returns the session user and a CSRF token when authentication is active,
 * otherwise returns {@code AUTH_REQUIRED}.
 */
@WebServlet("/api/auth/me")
public class MeServlet extends JsonServletSupport {

    /**
     * Handles {@code GET /api/auth/me}.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @throws IOException when writing fails
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            writeUnauthorized(response);
            return;
        }

        Object sessionUser = session.getAttribute("user");
        if (!(sessionUser instanceof AuthenticatedUser authenticatedUser)) {
            writeUnauthorized(response);
            return;
        }

        String csrfToken = CsrfTokenSupport.ensureToken(session);
        writeJsonResponse(response, HttpServletResponse.SC_OK, new AuthUserResponse(authenticatedUser, csrfToken));
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                "AUTH_REQUIRED", "Authentication required");
    }

}
