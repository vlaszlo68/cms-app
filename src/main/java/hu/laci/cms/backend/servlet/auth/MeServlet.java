package hu.laci.cms.backend.servlet.auth;

import hu.laci.cms.backend.config.session.AppSession;
import hu.laci.cms.backend.config.session.AppSessionManager;
import hu.laci.cms.backend.dto.auth.AuthenticatedUser;
import hu.laci.cms.backend.dto.auth.AuthUserResponse;
import hu.laci.cms.backend.servlet.support.JsonServletSupport;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

/**
 * Session restore endpoint for the currently authenticated user.
 * <p>
 * Returns the session user and a CSRF token when authentication is active,
 * otherwise returns {@code AUTH_REQUIRED}.
 */
@WebServlet(urlPatterns = "/api/auth/me", loadOnStartup = 1)
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
        Optional<AppSession> session = AppSessionManager.findSession(request, response);
        if (session.isEmpty()) {
            writeUnauthorized(response);
            return;
        }

        Optional<AuthenticatedUser> authenticatedUser = session.get().getAuthenticatedUser();
        if (authenticatedUser.isEmpty()) {
            writeUnauthorized(response);
            return;
        }

        String csrfToken = AppSessionManager.ensureCsrfToken(request, response, session.get());
        writeJsonResponse(response, HttpServletResponse.SC_OK,
                new AuthUserResponse(authenticatedUser.get(), csrfToken));
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                "AUTH_REQUIRED", "Authentication required");
    }

}
