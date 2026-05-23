package hu.laci.cms.backend.servlet.auth;

import com.google.gson.JsonSyntaxException;
import hu.laci.cms.backend.config.security.SecurityConfig;
import hu.laci.cms.backend.dao.common.DaoRegistry;
import hu.laci.cms.backend.dao.user.UserDao;
import hu.laci.cms.backend.dto.auth.RegisterRequest;
import hu.laci.cms.backend.dto.user.UserResponse;
import hu.laci.cms.backend.model.user.User;
import hu.laci.cms.backend.service.auth.CaptchaService;
import hu.laci.cms.backend.service.auth.RegistrationService;
import hu.laci.cms.backend.service.security.InMemoryRateLimiter;
import hu.laci.cms.backend.service.security.PasswordPolicyValidator;
import hu.laci.cms.backend.service.user.UserService;
import hu.laci.cms.backend.service.user.UserServiceException;
import hu.laci.cms.backend.servlet.support.JsonServletSupport;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.time.Duration;

/**
 * Public registration endpoint for inactive USER accounts awaiting admin approval.
 */
@WebServlet(name ="/api/auth/register", loadOnStartup = 1)
public class RegisterServlet extends JsonServletSupport {

    private static final int HTTP_TOO_MANY_REQUESTS = 429;

    private RegistrationService registrationService;
    private CaptchaService captchaService;
    private InMemoryRateLimiter registrationRateLimiter;
    private boolean registrationCaptchaEnabled;

    @Override
    public void init() throws ServletException {
        UserDao userDao = DaoRegistry.getDao(User.class);
        this.captchaService = new CaptchaService();
        this.registrationService = new RegistrationService(
                userDao,
                new PasswordPolicyValidator(SecurityConfig.getCurrent().getPasswordPolicy()),
                captchaService
        );
        this.registrationRateLimiter = new InMemoryRateLimiter(
                SecurityConfig.getCurrent().getMaxFailedAttempts(),
                Duration.ofMinutes(SecurityConfig.getCurrent().getLockMinutes())
        );
        this.registrationCaptchaEnabled = SecurityConfig.getCurrent().isRegistrationCaptchaEnabled();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String limiterKey = clientIp(request);
        if (registrationRateLimiter.isLocked(limiterKey)) {
            writeErrorResponse(response, HTTP_TOO_MANY_REQUESTS,
                    "RATE_LIMITED", "Too many registration attempts.");
            return;
        }

        try {
            RegisterRequest registerRequest = parseJson(request);
            HttpSession session = request.getSession(false);
            if (registrationCaptchaEnabled && !validateCaptcha(session, registerRequest)) {
                registrationRateLimiter.recordFailure(limiterKey);
                writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                        RegistrationService.CAPTCHA_INVALID, "Captcha validation failed.");
                return;
            }
            UserResponse registeredUser = registrationService.register(registerRequest, null, null, false);
            registrationRateLimiter.recordSuccess(limiterKey);
            writeJsonResponse(response, HttpServletResponse.SC_CREATED, registeredUser);
        } catch (BadRequestException e) {
            registrationRateLimiter.recordFailure(limiterKey);
            writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
        } catch (UserServiceException e) {
            registrationRateLimiter.recordFailure(limiterKey);
            writeServiceError(response, e);
        }
    }

    private boolean validateCaptcha(HttpSession session, RegisterRequest registerRequest) {
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
                expectedCaptchaAnswer, expectedCaptchaPurpose, createdAt, attempts,
                registerRequest == null ? null : registerRequest.getCaptchaId(),
                registerRequest == null ? null : registerRequest.getCaptchaAnswer(),
                CaptchaService.PURPOSE_REGISTRATION);
        updateCaptchaState(session, result);
        return result.valid();
    }

    private RegisterRequest parseJson(HttpServletRequest request) {
        try (BufferedReader reader = request.getReader()) {
            return gson.fromJson(reader, RegisterRequest.class);
        } catch (IOException | JsonSyntaxException e) {
            throw new BadRequestException("Invalid JSON request body.", e);
        }
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

    private void writeServiceError(HttpServletResponse response, UserServiceException e) throws IOException {
        int status = switch (e.getCode()) {
            case UserService.VALIDATION_ERROR -> HttpServletResponse.SC_BAD_REQUEST;
            case UserService.DUPLICATE_LOGIN_NAME, UserService.DUPLICATE_EMAIL_ADDRESS -> HttpServletResponse.SC_CONFLICT;
            case RegistrationService.CAPTCHA_INVALID -> HttpServletResponse.SC_BAD_REQUEST;
            default -> HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        };
        if (e.getValidationErrors().isEmpty()) {
            writeErrorResponse(response, status, e.getCode(), e.getMessage());
            return;
        }
        writeErrorResponse(response, status, e.getCode(), e.getMessage(), e.getValidationErrors());
    }

    private static String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private static final class BadRequestException extends RuntimeException {

        private BadRequestException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
