package hu.laci.cms.servlet;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;

@WebServlet("/api/auth/logout")
public class LogoutServlet extends JsonServletSupport {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        session.invalidate();

        writeJsonResponse(response, HttpServletResponse.SC_OK, Map.of("message", "Logged out"));
    }
}
