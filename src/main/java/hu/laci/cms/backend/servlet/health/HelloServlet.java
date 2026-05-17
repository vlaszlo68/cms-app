package hu.laci.cms.backend.servlet.health;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Minimal health/smoke-test servlet.
 */
@WebServlet("/hello")
public class HelloServlet extends HttpServlet {

    /**
     * Returns a simple JSON health response.
     *
     * @param req HTTP request
     * @param resp HTTP response
     * @throws ServletException when servlet handling fails
     * @throws IOException when writing fails
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(HttpServletResponse.SC_OK);
        PrintWriter writer = resp.getWriter();
        writer.write("{\"message\": \"Hello CMS\"}");
    }
}
