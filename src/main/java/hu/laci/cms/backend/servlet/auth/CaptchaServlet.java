package hu.laci.cms.backend.servlet.auth;

import hu.laci.cms.backend.config.session.AppSession;
import hu.laci.cms.backend.config.session.AppSessionManager;
import hu.laci.cms.backend.service.auth.CaptchaChallenge;
import hu.laci.cms.backend.service.auth.CaptchaService;
import hu.laci.cms.backend.service.security.RateLimiterManager;
import hu.laci.cms.backend.service.security.RequestRateLimiter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Locale;

/**
 * Public SVG CAPTCHA endpoint for the registration flow.
 */
@WebServlet(urlPatterns = "/api/auth/captcha", loadOnStartup = 1)
public class CaptchaServlet extends HttpServlet {

    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int MAX_GENERATION_ATTEMPTS = 5;
    private static final Duration GENERATION_LOCK_DURATION = Duration.ofMinutes(1);

    private final CaptchaService captchaService = new CaptchaService();
    private RequestRateLimiter generationRateLimiter;

    @Override
    public void init() throws ServletException {
        this.generationRateLimiter = RateLimiterManager.createRequestLimiter(
                "captcha_generation",
                MAX_GENERATION_ATTEMPTS,
                GENERATION_LOCK_DURATION);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        AppSession session = AppSessionManager.ensureSession(request, response);
        String limiterKey = request.getRemoteAddr() + ":" + session.getId();
        if (!generationRateLimiter.allowRequest(limiterKey)) {
            response.setStatus(HTTP_TOO_MANY_REQUESTS);
            response.setContentType("text/plain");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("Too many CAPTCHA requests.");
            return;
        }

        String purpose = resolvePurpose(request);
        CaptchaChallenge challenge = captchaService.createChallenge();
        AppSessionManager.storeCaptcha(request, response, new AppSessionManager.CaptchaState(
                challenge.getId(),
                challenge.getExpectedAnswer(),
                purpose,
                captchaService.currentTimeMillis(),
                0));

        response.setContentType("image/svg+xml");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("X-Captcha-Id", challenge.getId());
        response.getWriter().write(challenge.getSvg());
    }

    private String resolvePurpose(HttpServletRequest request) {
        String purpose = request.getParameter("purpose");
        if (purpose == null || purpose.trim().isEmpty()) {
            purpose = request.getParameter("context");
        }
        if (purpose == null || purpose.trim().isEmpty()) {
            return CaptchaService.PURPOSE_REGISTRATION;
        }

        String normalizedPurpose = purpose.trim().toLowerCase(Locale.ROOT);
        if (captchaService.isSupportedPurpose(normalizedPurpose)) {
            return normalizedPurpose;
        }
        return CaptchaService.PURPOSE_REGISTRATION;
    }
}
