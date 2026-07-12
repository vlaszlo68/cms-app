package hu.laci.cms.backend.config.session;

import hu.laci.cms.backend.config.database.DatabaseConfig;
import hu.laci.cms.backend.config.database.migration.DatabaseMigrationRunner;
import hu.laci.cms.backend.dto.auth.AuthenticatedUser;
import hu.laci.cms.backend.model.user.UserRole;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/** Database-backed integration tests for JDBC application-session persistence. */
class JdbcAppSessionStoreIntegrationTest {

    private static final String LOGIN_PREFIX = "jdbc_session_test_";

    private JdbcAppSessionStore store;

    @BeforeAll
    static void initializeDatabase() {
        DatabaseConfig.initialize(emptyServletContext());
        DatabaseMigrationRunner.runMigrations();
    }

    @AfterAll
    static void shutdownDatabase() {
        DatabaseConfig.shutdown();
    }

    @org.junit.jupiter.api.BeforeEach
    void setUp() throws SQLException {
        deleteTestSessions();
        AppSessionConfig config = AppSessionConfig.from(emptyServletContext(),
                Map.of(AppSessionConfig.ENV_STORE_MODE, "jdbc"));
        store = new JdbcAppSessionStore(config);
    }

    @AfterEach
    void cleanUp() throws SQLException {
        deleteTestSessions();
    }

    @Test
    void savesAndLoadsAuthenticatedSessionWithCaptchaAttribute() {
        String sessionId = "jdbc-session-save-load";
        AppSession session = session(sessionId, Instant.now().plusSeconds(600));
        AppSessionManager.CaptchaState captcha = new AppSessionManager.CaptchaState("captcha-id", 12,
                "registration", 1_000L, 1);
        session.putAttribute(AppSessionManager.toCaptchaAttribute(captcha));

        store.save(requestWithoutCookie(), Mockito.mock(HttpServletResponse.class), session);
        Optional<AppSession> loaded = store.find(requestWithCookie(sessionId), Mockito.mock(HttpServletResponse.class));

        Assertions.assertTrue(loaded.isPresent());
        AuthenticatedUser loadedUser = loaded.get().getAuthenticatedUser().orElseThrow();
        Assertions.assertEquals(77L, loadedUser.getId());
        Assertions.assertEquals(LOGIN_PREFIX + "user", loadedUser.getLoginName());
        Assertions.assertEquals("jdbc@example.com", loadedUser.getEmail());
        Assertions.assertEquals(UserRole.ADMIN, loadedUser.getRole());
        Assertions.assertEquals("csrf-token", loaded.get().getCsrfToken());
        Assertions.assertEquals(Optional.of(captcha), loaded.get().getAttribute(AppSessionManager.CAPTCHA_ATTRIBUTE_NAME)
                .map(AppSessionManager::fromCaptchaAttribute));
    }

    @Test
    void invalidationDeletesPersistedSessionAndClearsCookie() {
        String sessionId = "jdbc-session-invalidate";
        store.save(requestWithoutCookie(), Mockito.mock(HttpServletResponse.class), session(sessionId, Instant.now().plusSeconds(600)));
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        store.invalidate(requestWithCookie(sessionId), response);

        Assertions.assertTrue(store.find(requestWithCookie(sessionId), Mockito.mock(HttpServletResponse.class)).isEmpty());
        ArgumentCaptor<String> cookieHeader = ArgumentCaptor.forClass(String.class);
        Mockito.verify(response).addHeader(Mockito.eq("Set-Cookie"), cookieHeader.capture());
        Assertions.assertTrue(cookieHeader.getValue().contains("Max-Age=0"));
    }

    @Test
    void expiredSessionIsDeletedAndBrowserCookieIsCleared() {
        String sessionId = "jdbc-session-expired";
        store.save(requestWithoutCookie(), Mockito.mock(HttpServletResponse.class), session(sessionId, Instant.now().minusSeconds(1)));
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        Assertions.assertTrue(store.find(requestWithCookie(sessionId), response).isEmpty());

        ArgumentCaptor<String> cookieHeader = ArgumentCaptor.forClass(String.class);
        Mockito.verify(response).addHeader(Mockito.eq("Set-Cookie"), cookieHeader.capture());
        Assertions.assertTrue(cookieHeader.getValue().contains("Max-Age=0"));
    }

    private AppSession session(String sessionId, Instant expiresAt) {
        Instant now = Instant.now();
        AuthenticatedUser user = new AuthenticatedUser(77L, LOGIN_PREFIX + "user", "jdbc@example.com", UserRole.ADMIN);
        return new AppSession(sessionId, user, "csrf-token", now, now, expiresAt);
    }

    private HttpServletRequest requestWithoutCookie() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getCookies()).thenReturn(null);
        return request;
    }

    private HttpServletRequest requestWithCookie(String sessionId) {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("CMS_SESSION_ID", sessionId)});
        return request;
    }

    private void deleteTestSessions() throws SQLException {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM app_sessions WHERE login_name LIKE ?")) {
            statement.setString(1, LOGIN_PREFIX + "%");
            statement.executeUpdate();
        }
    }

    private static ServletContext emptyServletContext() {
        return (ServletContext) Proxy.newProxyInstance(ServletContext.class.getClassLoader(),
                new Class<?>[]{ServletContext.class}, (proxy, method, arguments) -> {
                    if ("getInitParameter".equals(method.getName())) {
                        return null;
                    }
                    Class<?> returnType = method.getReturnType();
                    if (returnType == boolean.class) {
                        return false;
                    }
                    if (returnType == int.class) {
                        return 0;
                    }
                    return null;
                });
    }
}
