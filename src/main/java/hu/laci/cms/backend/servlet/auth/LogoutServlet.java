package hu.laci.cms.backend.servlet.auth;

import hu.laci.cms.backend.servlet.support.JsonServletSupport;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;

/**
 * Logout endpoint that invalidates the current HTTP session.
 */
@WebServlet("/api/auth/logout")
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
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        writeJsonResponse(response, HttpServletResponse.SC_OK, Map.of("message", "Logged out"));
    }
}
