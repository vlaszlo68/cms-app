package hu.laci.cms.backend.config.session;

import hu.laci.cms.backend.dto.auth.AuthenticatedUser;
import hu.laci.cms.backend.service.auth.CaptchaService;
import hu.laci.cms.backend.servlet.support.CsrfTokenSupport;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Application session store backed by the servlet container's {@link HttpSession}.
 */
public class HttpSessionAppSessionStore implements AppSessionStore {

    private final AppSessionConfig config;

    /**
     * Creates an HTTP session backed store.
     *
     * @param config session configuration
     */
    public HttpSessionAppSessionStore(AppSessionConfig config) {
        this.config = config;
    }

    @Override
    public Optional<AppSession> find(HttpServletRequest request, HttpServletResponse response) {
        HttpSession httpSession = request.getSession(false);
        if (httpSession == null) {
            return Optional.empty();
        }
        return Optional.of(toAppSession(httpSession));
    }

    @Override
    public AppSession create(HttpServletRequest request, HttpServletResponse response) {
        HttpSession httpSession = request.getSession(true);
        return toAppSession(httpSession);
    }

    @Override
    public void save(HttpServletRequest request, HttpServletResponse response, AppSession session) {
        HttpSession httpSession = request.getSession(true);
        if (session.getAuthenticatedUser().isPresent()) {
            httpSession.setAttribute("user", session.getAuthenticatedUser().get());
        } else {
            httpSession.removeAttribute("user");
        }
        if (session.getCsrfToken() == null || session.getCsrfToken().isBlank()) {
            httpSession.removeAttribute(CsrfTokenSupport.SESSION_ATTRIBUTE);
        } else {
            httpSession.setAttribute(CsrfTokenSupport.SESSION_ATTRIBUTE, session.getCsrfToken());
        }

        Optional<AppSessionAttribute> captchaAttribute = session.getAttribute(AppSessionManager.CAPTCHA_ATTRIBUTE_NAME);
        if (captchaAttribute.isPresent()) {
            AppSessionManager.CaptchaState captchaState = AppSessionManager.fromCaptchaAttribute(captchaAttribute.get());
            httpSession.setAttribute(CaptchaService.SESSION_ID_ATTRIBUTE, captchaState.id());
            httpSession.setAttribute(CaptchaService.SESSION_ANSWER_ATTRIBUTE, captchaState.answer());
            httpSession.setAttribute(CaptchaService.SESSION_PURPOSE_ATTRIBUTE, captchaState.purpose());
            httpSession.setAttribute(CaptchaService.SESSION_CREATED_AT_ATTRIBUTE, captchaState.createdAt());
            httpSession.setAttribute(CaptchaService.SESSION_ATTEMPTS_ATTRIBUTE, captchaState.attempts());
        } else {
            clearCaptcha(httpSession);
        }
    }

    @Override
    public void invalidate(HttpServletRequest request, HttpServletResponse response) {
        HttpSession httpSession = request.getSession(false);
        if (httpSession != null) {
            httpSession.invalidate();
        }
    }

    private AppSession toAppSession(HttpSession httpSession) {
        Instant now = Instant.now();
        Instant createdAt = Instant.ofEpochMilli(httpSession.getCreationTime());
        Instant lastAccessedAt = Instant.ofEpochMilli(httpSession.getLastAccessedTime());
        Instant expiresAt = now.plus(Duration.ofMinutes(config.getTimeoutMinutes()));
        Object user = httpSession.getAttribute("user");
        Object csrfToken = httpSession.getAttribute(CsrfTokenSupport.SESSION_ATTRIBUTE);
        AppSession appSession = new AppSession(
                httpSession.getId(),
                user instanceof AuthenticatedUser authenticatedUser ? authenticatedUser : null,
                csrfToken instanceof String token ? token : null,
                createdAt,
                lastAccessedAt,
                expiresAt);

        AppSessionAttribute captchaAttribute = toCaptchaAttribute(httpSession);
        if (captchaAttribute != null) {
            appSession.putAttribute(captchaAttribute);
        }
        return appSession;
    }

    private AppSessionAttribute toCaptchaAttribute(HttpSession httpSession) {
        Object id = httpSession.getAttribute(CaptchaService.SESSION_ID_ATTRIBUTE);
        Object answer = httpSession.getAttribute(CaptchaService.SESSION_ANSWER_ATTRIBUTE);
        Object purpose = httpSession.getAttribute(CaptchaService.SESSION_PURPOSE_ATTRIBUTE);
        Object createdAt = httpSession.getAttribute(CaptchaService.SESSION_CREATED_AT_ATTRIBUTE);
        Object attempts = httpSession.getAttribute(CaptchaService.SESSION_ATTEMPTS_ATTRIBUTE);
        if (!(id instanceof String captchaId)
                || !(answer instanceof Integer captchaAnswer)
                || !(purpose instanceof String captchaPurpose)
                || !(createdAt instanceof Long captchaCreatedAt)) {
            return null;
        }

        int captchaAttempts = attempts instanceof Integer value ? value : 0;
        return AppSessionManager.toCaptchaAttribute(new AppSessionManager.CaptchaState(
                captchaId, captchaAnswer, captchaPurpose, captchaCreatedAt, captchaAttempts));
    }

    private void clearCaptcha(HttpSession httpSession) {
        httpSession.removeAttribute(CaptchaService.SESSION_ID_ATTRIBUTE);
        httpSession.removeAttribute(CaptchaService.SESSION_ANSWER_ATTRIBUTE);
        httpSession.removeAttribute(CaptchaService.SESSION_PURPOSE_ATTRIBUTE);
        httpSession.removeAttribute(CaptchaService.SESSION_CREATED_AT_ATTRIBUTE);
        httpSession.removeAttribute(CaptchaService.SESSION_ATTEMPTS_ATTRIBUTE);
    }
}
