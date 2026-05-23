package hu.laci.cms.backend.servlet.auth;

import com.google.gson.JsonSyntaxException;
import hu.laci.cms.backend.config.security.SecurityConfig;
import hu.laci.cms.backend.dao.common.DaoRegistry;
import hu.laci.cms.backend.dao.user.UserDao;
import hu.laci.cms.backend.dto.auth.AuthenticatedUser;
import hu.laci.cms.backend.dto.auth.AuthUserResponse;
import hu.laci.cms.backend.dto.auth.LoginRequest;
import hu.laci.cms.backend.model.user.User;
import hu.laci.cms.backend.service.AuthService;
import hu.laci.cms.backend.service.AuthServiceException;
import hu.laci.cms.backend.service.auth.CaptchaService;
import hu.laci.cms.backend.service.security.InMemoryRateLimiter;
import hu.laci.cms.backend.servlet.support.CsrfTokenSupport;
import hu.laci.cms.backend.servlet.support.JsonServletSupport;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

/**
 * Login endpoint for session-based authentication.
 * <p>
 * Successful login rotates the session id, stores an {@link AuthenticatedUser}
 * in the session, creates a CSRF token, and returns an {@link AuthUserResponse}
 * inside the common JSON response envelope.
 */
@WebServlet(name = "/api/auth/login", loadOnStartup = 1)
public class AuthServlet extends JsonServletSupport {

    private AuthService authService;
    private CaptchaService captchaService;
    private boolean loginCaptchaEnabled;

    /**
     * Resolves dependencies needed by the login endpoint.
     *
     * @throws ServletException when servlet initialization fails
     */
    @Override
    public void init() throws ServletException {
        UserDao userDao = DaoRegistry.getDao(User.class);
        this.authService = new AuthService(
                userDao,
                new InMemoryRateLimiter(
                        SecurityConfig.getCurrent().getMaxFailedAttempts(),
                        Duration.ofMinutes(SecurityConfig.getCurrent().getLockMinutes())
                )
        );
        this.captchaService = new CaptchaService();
        this.loginCaptchaEnabled = SecurityConfig.getCurrent().isLoginCaptchaEnabled();
    }

    /**
     * Handles {@code POST /api/auth/login}.
     *
     * @param request HTTP request containing a {@link LoginRequest} JSON body
     * @param response HTTP response
     * @throws IOException when reading or writing fails
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            LoginRequest loginRequest = parseLoginRequest(request);
            validateLoginRequest(loginRequest);
            if (loginCaptchaEnabled && !validateCaptcha(request, loginRequest)) {
                writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                        "CAPTCHA_INVALID", "Captcha validation failed.");
                return;
            }

            String loginName = loginRequest.getLoginName().trim();
            String password = loginRequest.getPassword();

            Optional<User> userOptional = authService.login(loginName, password, clientIp(request));
            if (userOptional.isEmpty()) {
                writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "INVALID_CREDENTIALS", "Invalid credentials");
                return;
            }

            User user = userOptional.get();
            AuthenticatedUser authenticatedUser = createSession(request, user);
            String csrfToken = CsrfTokenSupport.ensureToken(request.getSession(false));
            writeJsonResponse(response, HttpServletResponse.SC_OK, new AuthUserResponse(authenticatedUser, csrfToken));
        } catch (BadRequestException e) {
            writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
        } catch (AuthServiceException e) {
            writeErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "INTERNAL_ERROR", "Internal server error.");
        } catch (RuntimeException e) {
            writeErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "INTERNAL_ERROR", "Internal server error.");
        }
    }

    private LoginRequest parseLoginRequest(HttpServletRequest request) {
        try (BufferedReader reader = request.getReader()) {
            return gson.fromJson(reader, LoginRequest.class);
        } catch (IOException | JsonSyntaxException e) {
            throw new BadRequestException("Invalid JSON request body.", e);
        }
    }

    private void validateLoginRequest(LoginRequest loginRequest) {
        if (loginRequest == null
                || isBlank(loginRequest.getLoginName())
                || isBlank(loginRequest.getPassword())) {
            throw new BadRequestException("loginName and password are required.");
        }
        if (!isBlank(loginRequest.getCaptchaHoneypot())) {
            throw new BadRequestException("Invalid request.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private AuthenticatedUser createSession(HttpServletRequest request, User user) {
        HttpSession session = request.getSession(true);
        request.changeSessionId();

        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                user.getId(),
                user.getLoginName(),
                user.getEmailAddress(),
                user.getRole()
        );
        session.setAttribute("user", authenticatedUser);
        session.setAttribute(CsrfTokenSupport.SESSION_ATTRIBUTE, CsrfTokenSupport.createToken());
        return authenticatedUser;
    }

    private boolean validateCaptcha(HttpServletRequest request, LoginRequest loginRequest) {
        HttpSession session = request.getSession(false);
        String expectedCaptchaId = session == null
                ? null
                : (String) session.getAttribute(CaptchaService.SESSION_ID_ATTRIBUTE);
        Integer expectedCaptchaAnswer = session == null
                ? null
                : (Integer) session.getAttribute(CaptchaService.SESSION_ANSWER_ATTRIBUTE);
        String expectedCaptchaPurpose = session == null
                ? null
                : (String) session.getAttribute(CaptchaService.SESSION_PURPOSE_ATTRIBUTE);
        Long createdAt = session == null
                ? null
                : (Long) session.getAttribute(CaptchaService.SESSION_CREATED_AT_ATTRIBUTE);
        Integer attempts = session == null
                ? null
                : (Integer) session.getAttribute(CaptchaService.SESSION_ATTEMPTS_ATTRIBUTE);

        CaptchaService.CaptchaValidationResult result = captchaService.validateChallenge(expectedCaptchaId,
                expectedCaptchaAnswer, expectedCaptchaPurpose, createdAt, attempts, loginRequest.getCaptchaId(),
                loginRequest.getCaptchaAnswer(), CaptchaService.PURPOSE_LOGIN);
        updateCaptchaState(session, result);
        return result.valid();
    }

    private void clearCaptcha(HttpSession session) {
        if (session == null) {
            return;
        }
        session.removeAttribute(CaptchaService.SESSION_ID_ATTRIBUTE);
        session.removeAttribute(CaptchaService.SESSION_ANSWER_ATTRIBUTE);
        session.removeAttribute(CaptchaService.SESSION_PURPOSE_ATTRIBUTE);
        session.removeAttribute(CaptchaService.SESSION_CREATED_AT_ATTRIBUTE);
        session.removeAttribute(CaptchaService.SESSION_ATTEMPTS_ATTRIBUTE);
    }

    private void updateCaptchaState(HttpSession session, CaptchaService.CaptchaValidationResult result) {
        if (session == null) {
            return;
        }
        if (result.challengeConsumed()) {
            clearCaptcha(session);
            return;
        }
        session.setAttribute(CaptchaService.SESSION_ATTEMPTS_ATTRIBUTE, result.attemptsUsed());
    }

    private static String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private static final class BadRequestException extends RuntimeException {

        private BadRequestException(String message) {
            super(message);
        }

        private BadRequestException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
