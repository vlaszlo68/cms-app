package hu.laci.cms.backend.config.session;

import hu.laci.cms.backend.dto.auth.AuthenticatedUser;
import hu.laci.cms.backend.model.user.UserRole;
import hu.laci.cms.backend.servlet.support.CsrfTokenSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Unit tests for session identifiers, cookies, HTTP-backed state, and session-manager caching. */
class SessionInfrastructureTest {

    @AfterEach
    void resetSessionManager() {
        AppSessionConfig.reset();
        AppSessionManager.reset();
        SessionContext.clear();
    }

    @Test
    void sessionIdGeneratorProducesDistinctUrlSafe256BitIds() {
        String first = SessionIdGenerator.generate();
        String second = SessionIdGenerator.generate();

        Assertions.assertEquals(43, first.length());
        Assertions.assertTrue(first.matches("[A-Za-z0-9_-]+"));
        Assertions.assertNotEquals(first, second);
    }

    @Test
    void cookieSupportReadsWritesAndClearsConfiguredCookie() {
        SessionCookieSupport cookies = new SessionCookieSupport(AppSessionConfig.getCurrent());
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        Mockito.when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("other", "x"),
                new Cookie("CMS_SESSION_ID", "session-id")});

        Assertions.assertEquals(Optional.of("session-id"), cookies.readSessionId(request));
        cookies.addSessionCookie(response, "session-id");
        cookies.clearSessionCookie(response);

        ArgumentCaptor<String> headerValues = ArgumentCaptor.forClass(String.class);
        Mockito.verify(response, Mockito.times(2)).addHeader(Mockito.eq("Set-Cookie"), headerValues.capture());
        Assertions.assertTrue(headerValues.getAllValues().get(0).contains("CMS_SESSION_ID=session-id"));
        Assertions.assertTrue(headerValues.getAllValues().get(0).contains("HttpOnly"));
        Assertions.assertTrue(headerValues.getAllValues().get(1).contains("Max-Age=0"));
    }

    @Test
    void httpSessionStoreMapsAndPersistsUserAndCsrfToken() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        HttpSession httpSession = Mockito.mock(HttpSession.class);
        Mockito.when(request.getSession(true)).thenReturn(httpSession);
        Mockito.when(httpSession.getId()).thenReturn("http-session");
        Mockito.when(httpSession.getCreationTime()).thenReturn(1_000L);
        Mockito.when(httpSession.getLastAccessedTime()).thenReturn(2_000L);
        AuthenticatedUser user = user();
        AppSession session = new AppSession("http-session", user, "csrf-token", Instant.ofEpochMilli(1_000L),
                Instant.ofEpochMilli(2_000L), Instant.now().plusSeconds(60));
        HttpSessionAppSessionStore store = new HttpSessionAppSessionStore(AppSessionConfig.getCurrent());

        store.save(request, response, session);

        Mockito.verify(httpSession).setAttribute("user", user);
        Mockito.verify(httpSession).setAttribute(CsrfTokenSupport.SESSION_ATTRIBUTE, "csrf-token");
        Mockito.verify(httpSession).removeAttribute("registrationCaptchaId");
    }

    @Test
    void appSessionManagerCreatesAndCachesAuthenticatedSession() {
        AppSessionManager.reset();
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        HttpSession httpSession = Mockito.mock(HttpSession.class);
        Map<String, Object> requestAttributes = new HashMap<>();
        Mockito.when(request.getAttribute(Mockito.anyString()))
                .thenAnswer(invocation -> requestAttributes.get(invocation.getArgument(0, String.class)));
        Mockito.doAnswer(invocation -> {
            requestAttributes.put(invocation.getArgument(0, String.class), invocation.getArgument(1));
            return null;
        }).when(request).setAttribute(Mockito.anyString(), Mockito.any());
        Mockito.when(request.getSession(false)).thenReturn(null);
        Mockito.when(request.getSession(true)).thenReturn(httpSession);
        Mockito.when(httpSession.getId()).thenReturn("http-session");
        Mockito.when(httpSession.getCreationTime()).thenReturn(1_000L);
        Mockito.when(httpSession.getLastAccessedTime()).thenReturn(2_000L);
        AuthenticatedUser user = user();

        AppSession created = AppSessionManager.createAuthenticatedSession(request, response, user);
        Optional<AppSession> cached = AppSessionManager.findSession(request, response);

        Assertions.assertEquals(Optional.of(user), created.getAuthenticatedUser());
        Assertions.assertFalse(created.getCsrfToken().isBlank());
        Assertions.assertEquals(Optional.of(created), cached);
        Mockito.verify(request).setAttribute(Mockito.contains(".session"), Mockito.eq(created));
        Mockito.verify(httpSession).setAttribute(Mockito.eq("user"), Mockito.eq(user));
    }

    @Test
    void httpSessionStoreInvalidatesExistingSessionAndIgnoresMissingOne() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        HttpSession httpSession = Mockito.mock(HttpSession.class);
        Mockito.when(request.getSession(false)).thenReturn(httpSession);
        HttpSessionAppSessionStore store = new HttpSessionAppSessionStore(AppSessionConfig.getCurrent());

        store.invalidate(request, response);

        Mockito.verify(httpSession).invalidate();
    }

    @Test
    void captchaValidationUpdatesAttemptsThenRemovesConsumedChallenge() throws Exception {
        AppSession session = new AppSession("test-session", null, null, Instant.now(), Instant.now(),
                Instant.now().plusSeconds(600));
        CapturingStore store = new CapturingStore(session);
        setManagerStore(store);
        HttpServletRequest request = cachedRequest();
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        AppSessionManager.CaptchaState captcha = new AppSessionManager.CaptchaState("captcha", 12, "registration",
                1_000L, 0);

        AppSessionManager.storeCaptcha(request, response, captcha);
        AppSessionManager.updateCaptchaAfterValidation(request, response,
                new hu.laci.cms.backend.service.auth.CaptchaService.CaptchaValidationResult(false, 1, false));

        Assertions.assertEquals(1, AppSessionManager.findCaptcha(request, response).orElseThrow().attempts());
        AppSessionManager.updateCaptchaAfterValidation(request, response,
                new hu.laci.cms.backend.service.auth.CaptchaService.CaptchaValidationResult(false, 2, true));
        Assertions.assertTrue(AppSessionManager.findCaptcha(request, response).isEmpty());
        Assertions.assertEquals(3, store.saveCalls);
    }

    private AuthenticatedUser user() {
        return new AuthenticatedUser(5L, "tester", "tester@example.com", UserRole.ADMIN);
    }

    private HttpServletRequest cachedRequest() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Map<String, Object> attributes = new HashMap<>();
        Mockito.when(request.getAttribute(Mockito.anyString()))
                .thenAnswer(invocation -> attributes.get(invocation.getArgument(0, String.class)));
        Mockito.doAnswer(invocation -> {
            attributes.put(invocation.getArgument(0, String.class), invocation.getArgument(1));
            return null;
        }).when(request).setAttribute(Mockito.anyString(), Mockito.any());
        return request;
    }

    private void setManagerStore(AppSessionStore store) throws ReflectiveOperationException {
        java.lang.reflect.Field field = AppSessionManager.class.getDeclaredField("store");
        field.setAccessible(true);
        field.set(null, store);
    }

    private static final class CapturingStore implements AppSessionStore {

        private final AppSession session;
        private int saveCalls;

        private CapturingStore(AppSession session) {
            this.session = session;
        }

        @Override
        public Optional<AppSession> find(HttpServletRequest request, HttpServletResponse response) {
            return Optional.of(session);
        }

        @Override
        public AppSession create(HttpServletRequest request, HttpServletResponse response) {
            return session;
        }

        @Override
        public void save(HttpServletRequest request, HttpServletResponse response, AppSession session) {
            saveCalls++;
        }

        @Override
        public void invalidate(HttpServletRequest request, HttpServletResponse response) {
        }
    }
}
