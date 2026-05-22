package hu.laci.cms.backend.servlet.auth;

import hu.laci.cms.backend.service.auth.CaptchaChallenge;
import hu.laci.cms.backend.service.auth.CaptchaService;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Public SVG CAPTCHA endpoint for the registration flow.
 */
@WebServlet("/api/auth/captcha")
public class CaptchaServlet extends HttpServlet {

    private final CaptchaService captchaService = new CaptchaService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        CaptchaChallenge challenge = captchaService.createChallenge();
        HttpSession session = request.getSession(true);
        session.setAttribute(CaptchaService.SESSION_ID_ATTRIBUTE, challenge.getId());
        session.setAttribute(CaptchaService.SESSION_ANSWER_ATTRIBUTE, challenge.getExpectedAnswer());

        response.setContentType("image/svg+xml");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("X-Captcha-Id", challenge.getId());
        response.getWriter().write(challenge.getSvg());
    }
}
