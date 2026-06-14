package hu.laci.cms.backend.config.session;

import com.google.gson.Gson;
import hu.laci.cms.backend.dto.auth.AuthenticatedUser;
import hu.laci.cms.backend.service.auth.CaptchaService;
import hu.laci.cms.backend.servlet.support.CsrfTokenSupport;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Facade used by servlet and filter code to access application session state.
 */
public final class AppSessionManager {

    public static final String CAPTCHA_ATTRIBUTE_NAME = "captcha";

    private static final String REQUEST_ATTRIBUTE = AppSessionManager.class.getName() + ".session";
    private static final Gson GSON = new Gson();
    private static volatile AppSessionStore store = new HttpSessionAppSessionStore(AppSessionConfig.getCurrent());
    private static volatile AppSessionConfig config = AppSessionConfig.getCurrent();

    private AppSessionManager() {
    }

    /**
     * Initializes the backing store for the current configuration.
     *
     * @param appSessionConfig session configuration
     */
    public static void initialize(AppSessionConfig appSessionConfig) {
        config = appSessionConfig;
        store = switch (appSessionConfig.getStoreMode()) {
            case HTTP -> new HttpSessionAppSessionStore(appSessionConfig);
            case JDBC -> new JdbcAppSessionStore(appSessionConfig);
            case REDIS -> throw new IllegalStateException("Redis session store is planned but not implemented.");
        };
    }

    public static void reset() {
        config = AppSessionConfig.getCurrent();
        store = new HttpSessionAppSessionStore(config);
    }

    /**
     * Finds the current request's session and caches it for the request.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @return session when present and valid
     */
    public static Optional<AppSession> findSession(HttpServletRequest request, HttpServletResponse response) {
        Object cached = request.getAttribute(REQUEST_ATTRIBUTE);
        if (cached instanceof AppSession appSession) {
            return Optional.of(appSession);
        }

        Optional<AppSession> session = store.find(request, response);
        session.ifPresent(appSession -> request.setAttribute(REQUEST_ATTRIBUTE, appSession));
        return session;
    }

    /**
     * Ensures that a session exists for the current request.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @return existing or newly created session
     */
    public static AppSession ensureSession(HttpServletRequest request, HttpServletResponse response) {
        return findSession(request, response).orElseGet(() -> {
            AppSession createdSession = store.create(request, response);
            request.setAttribute(REQUEST_ATTRIBUTE, createdSession);
            return createdSession;
        });
    }

    /**
     * Creates a fresh authenticated session.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @param authenticatedUser authenticated user snapshot
     * @return created session
     */
    public static AppSession createAuthenticatedSession(HttpServletRequest request, HttpServletResponse response,
                                                        AuthenticatedUser authenticatedUser) {
        invalidate(request, response);
        AppSession session = store.create(request, response);
        session.setAuthenticatedUser(authenticatedUser);
        session.setCsrfToken(CsrfTokenSupport.createToken());
        touch(session);
        store.save(request, response, session);
        request.setAttribute(REQUEST_ATTRIBUTE, session);
        return session;
    }

    /**
     * Invalidates the current request's session.
     *
     * @param request HTTP request
     * @param response HTTP response
     */
    public static void invalidate(HttpServletRequest request, HttpServletResponse response) {
        store.invalidate(request, response);
        request.removeAttribute(REQUEST_ATTRIBUTE);
    }

    /**
     * Returns the current authenticated user.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @return authenticated user when present
     */
    public static Optional<AuthenticatedUser> getAuthenticatedUser(HttpServletRequest request,
                                                                   HttpServletResponse response) {
        return findSession(request, response).flatMap(AppSession::getAuthenticatedUser);
    }

    /**
     * Ensures a CSRF token in the given session.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @param session session to update
     * @return CSRF token
     */
    public static String ensureCsrfToken(HttpServletRequest request, HttpServletResponse response, AppSession session) {
        if (session.getCsrfToken() != null && !session.getCsrfToken().isBlank()) {
            return session.getCsrfToken();
        }

        session.setCsrfToken(CsrfTokenSupport.createToken());
        touch(session);
        store.save(request, response, session);
        return session.getCsrfToken();
    }

    /**
     * Stores CAPTCHA state in the current session.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @param captchaState CAPTCHA state
     * @return session containing the stored state
     */
    public static AppSession storeCaptcha(HttpServletRequest request, HttpServletResponse response,
                                          CaptchaState captchaState) {
        AppSession session = ensureSession(request, response);
        session.putAttribute(toCaptchaAttribute(captchaState));
        touch(session);
        store.save(request, response, session);
        return session;
    }

    /**
     * Finds CAPTCHA state in the current session.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @return CAPTCHA state when present
     */
    public static Optional<CaptchaState> findCaptcha(HttpServletRequest request, HttpServletResponse response) {
        return findSession(request, response)
                .flatMap(session -> session.getAttribute(CAPTCHA_ATTRIBUTE_NAME))
                .map(AppSessionManager::fromCaptchaAttribute);
    }

    public static boolean validateCaptcha(HttpServletRequest request, HttpServletResponse response,
                                          CaptchaService captchaService, String submittedCaptchaId,
                                          String submittedCaptchaAnswer, String requiredPurpose) {
        Optional<CaptchaState> captchaState = findCaptcha(request, response);
        CaptchaService.CaptchaValidationResult result = captchaService.validateChallenge(
                captchaState.map(CaptchaState::id).orElse(null),
                captchaState.map(CaptchaState::answer).orElse(null),
                captchaState.map(CaptchaState::purpose).orElse(null),
                captchaState.map(CaptchaState::createdAt).orElse(null),
                captchaState.map(CaptchaState::attempts).orElse(null),
                submittedCaptchaId,
                submittedCaptchaAnswer,
                requiredPurpose
        );
        updateCaptchaAfterValidation(request, response, result);
        return result.valid();
    }

    /**
     * Updates or clears CAPTCHA state after validation.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @param result validation result
     */
    public static void updateCaptchaAfterValidation(HttpServletRequest request, HttpServletResponse response,
                                                    hu.laci.cms.backend.service.auth.CaptchaService.CaptchaValidationResult result) {
        Optional<AppSession> sessionOptional = findSession(request, response);
        if (sessionOptional.isEmpty()) {
            return;
        }

        AppSession session = sessionOptional.get();
        if (result.challengeConsumed()) {
            session.removeAttribute(CAPTCHA_ATTRIBUTE_NAME);
        } else {
            Optional<CaptchaState> captchaState = session.getAttribute(CAPTCHA_ATTRIBUTE_NAME)
                    .map(AppSessionManager::fromCaptchaAttribute);
            captchaState.ifPresent(state -> session.putAttribute(toCaptchaAttribute(new CaptchaState(
                    state.id(), state.answer(), state.purpose(), state.createdAt(), result.attemptsUsed()))));
        }
        touch(session);
        store.save(request, response, session);
    }

    static AppSessionAttribute toCaptchaAttribute(CaptchaState captchaState) {
        Instant now = Instant.now();
        return new AppSessionAttribute(CAPTCHA_ATTRIBUTE_NAME, AppSessionAttributeType.CAPTCHA_STATE,
                GSON.toJson(captchaState), now, now);
    }

    static CaptchaState fromCaptchaAttribute(AppSessionAttribute attribute) {
        return GSON.fromJson(attribute.getJsonValue(), CaptchaState.class);
    }

    private static void touch(AppSession session) {
        Instant now = Instant.now();
        session.setLastAccessedAt(now);
        session.setExpiresAt(now.plus(Duration.ofMinutes(config.getTimeoutMinutes())));
    }

    /**
     * Structured CAPTCHA state stored as a typed session attribute.
     *
     * @param id challenge id
     * @param answer expected answer
     * @param purpose challenge purpose
     * @param createdAt creation time in epoch milliseconds
     * @param attempts attempts used so far
     */
    public record CaptchaState(String id, Integer answer, String purpose, Long createdAt, Integer attempts) {
    }
}
