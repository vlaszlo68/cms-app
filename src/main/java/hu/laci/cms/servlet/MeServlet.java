package hu.laci.cms.servlet;

import hu.laci.cms.model.User;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;

@WebServlet("/api/auth/me")
public class MeServlet extends JsonServletSupport {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            writeUnauthorized(response);
            return;
        }

        Object sessionUser = session.getAttribute("user");
        if (!(sessionUser instanceof User user)) {
            writeUnauthorized(response);
            return;
        }

        writeJsonResponse(response, HttpServletResponse.SC_OK, new MeResponse(
                user.getId(),
                user.getLoginName(),
                user.getEmailAddress()
        ));
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        writeJsonResponse(response, HttpServletResponse.SC_UNAUTHORIZED, Map.of("error", "Not authenticated"));
    }

    private static final class MeResponse {

        private final Long id;
        private final String loginName;
        private final String email;

        private MeResponse(Long id, String loginName, String email) {
            this.id = id;
            this.loginName = loginName;
            this.email = email;
        }
    }
}
