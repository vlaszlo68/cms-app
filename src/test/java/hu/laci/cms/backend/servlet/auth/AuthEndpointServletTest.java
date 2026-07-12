package hu.laci.cms.backend.servlet.auth;

import hu.laci.cms.backend.config.security.SecurityConfig;
import hu.laci.cms.backend.config.session.AppSession;
import hu.laci.cms.backend.config.session.AppSessionManager;
import hu.laci.cms.backend.dto.auth.AuthenticatedUser;
import hu.laci.cms.backend.dto.user.UserResponse;
import hu.laci.cms.backend.model.user.RegistrationState;
import hu.laci.cms.backend.model.user.User;
import hu.laci.cms.backend.model.user.UserRole;
import hu.laci.cms.backend.service.AuthService;
import hu.laci.cms.backend.service.auth.CaptchaService;
import hu.laci.cms.backend.service.auth.RegistrationService;
import hu.laci.cms.backend.service.security.AttemptRateLimiter;
import hu.laci.cms.backend.service.security.RequestRateLimiter;
import hu.laci.cms.backend.servlet.support.ServletTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Optional;

/**
 * Unit tests for the public authentication servlet endpoints.
 */
class AuthEndpointServletTest {

    @AfterEach
    void resetSecurityConfig() {
        SecurityConfig.reset();
    }

    @Test
    void loginRejectsMissingCredentialsBeforeAuthentication() throws Exception {
        AuthServlet servlet = new AuthServlet();
        AuthService authService = Mockito.mock(AuthService.class);
        setField(servlet, "authService", authService);
        setField(servlet, "loginCaptchaEnabled", false);
        HttpServletRequest request = ServletTestSupport.request().withJsonBody("{}").build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();

        servlet.doPost(request, response.build());

        Assertions.assertEquals(400, response.getStatus());
        Assertions.assertTrue(response.getBody().contains("INVALID_REQUEST"));
        Mockito.verifyNoInteractions(authService);
    }

    @Test
    void loginRejectsMalformedJsonBeforeAuthentication() throws Exception {
        AuthServlet servlet = new AuthServlet();
        AuthService authService = Mockito.mock(AuthService.class);
        setField(servlet, "authService", authService);
        setField(servlet, "loginCaptchaEnabled", false);
        HttpServletRequest request = ServletTestSupport.request().withJsonBody("{").build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();

        servlet.doPost(request, response.build());

        Assertions.assertEquals(400, response.getStatus());
        Assertions.assertTrue(response.getBody().contains("INVALID_REQUEST"));
        Mockito.verifyNoInteractions(authService);
    }

    @Test
    void loginReturnsAuthenticatedUserAndCsrfToken() throws Exception {
        AuthServlet servlet = new AuthServlet();
        AuthService authService = Mockito.mock(AuthService.class);
        User user = user();
        AppSession session = session(user, null);
        setField(servlet, "authService", authService);
        setField(servlet, "loginCaptchaEnabled", false);
        Mockito.when(authService.login("tester", "Password123!", "127.0.0.1")).thenReturn(Optional.of(user));
        HttpServletRequest request = ServletTestSupport.request()
                .withJsonBody("{\"loginName\":\"tester\",\"password\":\"Password123!\"}")
                .withRemoteAddress("127.0.0.1")
                .build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();

        try (MockedStatic<AppSessionManager> sessions = Mockito.mockStatic(AppSessionManager.class)) {
            sessions.when(() -> AppSessionManager.createAuthenticatedSession(
                    Mockito.eq(request), Mockito.eq(response.build()), Mockito.any(AuthenticatedUser.class)))
                    .thenReturn(session);
            sessions.when(() -> AppSessionManager.ensureCsrfToken(request, response.build(), session))
                    .thenReturn("csrf-token");

            servlet.doPost(request, response.build());
        }

        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertTrue(response.getBody().contains("csrf-token"));
        Assertions.assertTrue(response.getBody().contains("tester"));
    }

    @Test
    void loginReturnsUnauthorizedWhenCredentialsAreRejected() throws Exception {
        AuthServlet servlet = new AuthServlet();
        AuthService authService = Mockito.mock(AuthService.class);
        setField(servlet, "authService", authService);
        setField(servlet, "loginCaptchaEnabled", false);
        Mockito.when(authService.login("tester", "wrong", "127.0.0.1")).thenReturn(Optional.empty());
        HttpServletRequest request = ServletTestSupport.request()
                .withJsonBody("{\"loginName\":\"tester\",\"password\":\"wrong\"}")
                .withRemoteAddress("127.0.0.1")
                .build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();

        servlet.doPost(request, response.build());

        Assertions.assertEquals(401, response.getStatus());
        Assertions.assertTrue(response.getBody().contains("INVALID_CREDENTIALS"));
    }

    @Test
    void logoutInvalidatesSessionAndReturnsSuccessEnvelope() throws Exception {
        LogoutServlet servlet = new LogoutServlet();
        HttpServletRequest request = ServletTestSupport.request().build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();

        try (MockedStatic<AppSessionManager> sessions = Mockito.mockStatic(AppSessionManager.class)) {
            servlet.doPost(request, response.build());
            sessions.verify(() -> AppSessionManager.invalidate(request, response.build()));
        }

        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertTrue(response.getBody().contains("Logged out"));
    }

    @Test
    void meReturnsUnauthorizedWhenNoSessionExists() throws Exception {
        MeServlet servlet = new MeServlet();
        HttpServletRequest request = ServletTestSupport.request().build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();

        try (MockedStatic<AppSessionManager> sessions = Mockito.mockStatic(AppSessionManager.class)) {
            sessions.when(() -> AppSessionManager.findSession(request, response.build())).thenReturn(Optional.empty());
            servlet.doGet(request, response.build());
        }

        Assertions.assertEquals(401, response.getStatus());
        Assertions.assertTrue(response.getBody().contains("AUTH_REQUIRED"));
    }

    @Test
    void meReturnsSessionUserAndCsrfToken() throws Exception {
        MeServlet servlet = new MeServlet();
        HttpServletRequest request = ServletTestSupport.request().build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();
        AppSession session = session(user(), "existing-token");

        try (MockedStatic<AppSessionManager> sessions = Mockito.mockStatic(AppSessionManager.class)) {
            sessions.when(() -> AppSessionManager.findSession(request, response.build())).thenReturn(Optional.of(session));
            sessions.when(() -> AppSessionManager.ensureCsrfToken(request, response.build(), session))
                    .thenReturn("csrf-token");
            servlet.doGet(request, response.build());
        }

        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertTrue(response.getBody().contains("tester"));
        Assertions.assertTrue(response.getBody().contains("csrf-token"));
    }

    @Test
    void authConfigReturnsDefaultFeatureFlags() throws Exception {
        AuthConfigServlet servlet = new AuthConfigServlet();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();

        servlet.doGet(ServletTestSupport.request().build(), response.build());

        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertTrue(response.getBody().contains("loginCaptchaEnabled"));
        Assertions.assertTrue(response.getBody().contains("registrationCaptchaEnabled"));
    }

    @Test
    void captchaReturnsRateLimitedResponseWhenGenerationIsDenied() throws Exception {
        CaptchaServlet servlet = new CaptchaServlet();
        RequestRateLimiter limiter = Mockito.mock(RequestRateLimiter.class);
        Mockito.when(limiter.allowRequest("127.0.0.1:session-id")).thenReturn(false);
        setField(servlet, "generationRateLimiter", limiter);
        HttpServletRequest request = ServletTestSupport.request().withRemoteAddress("127.0.0.1").build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();
        AppSession session = session(user(), null);

        try (MockedStatic<AppSessionManager> sessions = Mockito.mockStatic(AppSessionManager.class)) {
            sessions.when(() -> AppSessionManager.ensureSession(request, response.build())).thenReturn(session);
            servlet.doGet(request, response.build());
        }

        Assertions.assertEquals(429, response.getStatus());
        Assertions.assertEquals("text/plain", response.getContentType());
    }

    @Test
    void captchaReturnsSvgAndChallengeHeaderWhenGenerationIsAllowed() throws Exception {
        CaptchaServlet servlet = new CaptchaServlet();
        RequestRateLimiter limiter = Mockito.mock(RequestRateLimiter.class);
        Mockito.when(limiter.allowRequest("127.0.0.1:session-id")).thenReturn(true);
        setField(servlet, "generationRateLimiter", limiter);
        HttpServletRequest request = ServletTestSupport.request().withRemoteAddress("127.0.0.1")
                .withParameter("purpose", "login").build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();
        AppSession session = session(user(), null);

        try (MockedStatic<AppSessionManager> sessions = Mockito.mockStatic(AppSessionManager.class)) {
            sessions.when(() -> AppSessionManager.ensureSession(request, response.build())).thenReturn(session);
            servlet.doGet(request, response.build());
        }

        Assertions.assertEquals("image/svg+xml", response.getContentType());
        Assertions.assertEquals("UTF-8", response.getCharacterEncoding());
        Assertions.assertNotNull(response.getHeader("X-Captcha-Id"));
        Assertions.assertTrue(response.getBody().contains("<svg"));
    }

    @Test
    void registerReturnsCreatedUserWhenCaptchaIsDisabled() throws Exception {
        RegisterServlet servlet = new RegisterServlet();
        RegistrationService registrationService = Mockito.mock(RegistrationService.class);
        AttemptRateLimiter limiter = Mockito.mock(AttemptRateLimiter.class);
        UserResponse registeredUser = new UserResponse(9L, "new-user", "New User", "new@example.com",
                UserRole.USER, false, RegistrationState.PENDING, null, null);
        setField(servlet, "registrationService", registrationService);
        setField(servlet, "registrationRateLimiter", limiter);
        setField(servlet, "registrationCaptchaEnabled", false);
        Mockito.when(limiter.isLocked("127.0.0.1")).thenReturn(false);
        Mockito.when(registrationService.register(Mockito.any(), Mockito.isNull(), Mockito.isNull(), Mockito.eq(false)))
                .thenReturn(registeredUser);
        HttpServletRequest request = ServletTestSupport.request()
                .withRemoteAddress("127.0.0.1")
                .withJsonBody("{\"loginName\":\"new-user\",\"userName\":\"New User\","
                        + "\"emailAddress\":\"new@example.com\",\"password\":\"Password123!\"}")
                .build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();

        servlet.doPost(request, response.build());

        Assertions.assertEquals(201, response.getStatus());
        Assertions.assertTrue(response.getBody().contains("new-user"));
        Mockito.verify(limiter).recordSuccess("127.0.0.1");
    }

    @Test
    void registerReturnsRateLimitedBeforeParsingRequest() throws Exception {
        RegisterServlet servlet = new RegisterServlet();
        AttemptRateLimiter limiter = Mockito.mock(AttemptRateLimiter.class);
        Mockito.when(limiter.isLocked("127.0.0.1")).thenReturn(true);
        setField(servlet, "registrationRateLimiter", limiter);
        HttpServletRequest request = ServletTestSupport.request().withRemoteAddress("127.0.0.1").build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();

        servlet.doPost(request, response.build());

        Assertions.assertEquals(429, response.getStatus());
        Assertions.assertTrue(response.getBody().contains("RATE_LIMITED"));
    }

    private User user() {
        return new User(1L, "Tester", "tester", "tester@example.com", "hash", UserRole.ADMIN,
                Boolean.TRUE, RegistrationState.COMPLETED);
    }

    private AppSession session(User user, String csrfToken) {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(user.getId(), user.getLoginName(),
                user.getEmailAddress(), user.getRole());
        Instant now = Instant.now();
        return new AppSession("session-id", authenticatedUser, csrfToken, now, now, now.plusSeconds(600));
    }

    private void setField(Object target, String fieldName, Object value) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
