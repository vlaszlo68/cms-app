package hu.laci.cms.backend.servlet.auth;

import hu.laci.cms.backend.config.session.AppSessionManager;
import hu.laci.cms.backend.servlet.support.JsonServletSupport;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * Logout endpoint that invalidates the current HTTP session.
 */
@WebServlet(urlPatterns = "/api/auth/logout", loadOnStartup = 1)
public class LogoutServlet extends JsonServletSupport {

    /**
     * Handles {@code POST /api/auth/logout}.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @throws IOException when writing fails
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        AppSessionManager.invalidate(request, response);

        writeJsonResponse(response, HttpServletResponse.SC_OK, Map.of("message", "Logged out"));
    }
}
