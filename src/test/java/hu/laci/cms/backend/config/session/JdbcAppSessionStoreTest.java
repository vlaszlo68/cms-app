package hu.laci.cms.backend.config.session;

import hu.laci.cms.backend.config.database.DatabaseConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.MockedStatic;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Unit tests for JDBC-session-store behavior that does not require a database connection. */
class JdbcAppSessionStoreTest {

    @Test
    void findReturnsEmptyWithoutSessionCookieBeforeAccessingDatabase() {
        JdbcAppSessionStore store = new JdbcAppSessionStore(AppSessionConfig.getCurrent());
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        Mockito.when(request.getCookies()).thenReturn(null);

        Assertions.assertTrue(store.find(request, response).isEmpty());
        Mockito.verifyNoInteractions(response);
    }

    @Test
    void invalidateWithoutCookieStillClearsBrowserCookie() {
        JdbcAppSessionStore store = new JdbcAppSessionStore(AppSessionConfig.getCurrent());
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        Mockito.when(request.getCookies()).thenReturn(null);

        store.invalidate(request, response);

        ArgumentCaptor<String> value = ArgumentCaptor.forClass(String.class);
        Mockito.verify(response).addHeader(Mockito.eq("Set-Cookie"), value.capture());
        Assertions.assertTrue(value.getValue().contains("CMS_SESSION_ID="));
        Assertions.assertTrue(value.getValue().contains("Max-Age=0"));
    }

    @Test
    void findWrapsDatabaseConnectionFailure() throws Exception {
        JdbcAppSessionStore store = new JdbcAppSessionStore(AppSessionConfig.getCurrent());
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        Mockito.when(request.getCookies()).thenReturn(new javax.servlet.http.Cookie[]{
                new javax.servlet.http.Cookie("CMS_SESSION_ID", "session-id")});

        try (MockedStatic<DatabaseConfig> database = Mockito.mockStatic(DatabaseConfig.class)) {
            database.when(DatabaseConfig::getConnection).thenThrow(new java.sql.SQLException("unavailable"));

            IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class,
                    () -> store.find(request, response));

            Assertions.assertTrue(exception.getMessage().contains("load JDBC-backed session"));
        }
    }

    @Test
    void invalidateWrapsDatabaseConnectionFailureBeforeCookieCleanup() throws Exception {
        JdbcAppSessionStore store = new JdbcAppSessionStore(AppSessionConfig.getCurrent());
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        Mockito.when(request.getCookies()).thenReturn(new javax.servlet.http.Cookie[]{
                new javax.servlet.http.Cookie("CMS_SESSION_ID", "session-id")});

        try (MockedStatic<DatabaseConfig> database = Mockito.mockStatic(DatabaseConfig.class)) {
            database.when(DatabaseConfig::getConnection).thenThrow(new java.sql.SQLException("unavailable"));

            IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class,
                    () -> store.invalidate(request, response));

            Assertions.assertTrue(exception.getMessage().contains("invalidate JDBC-backed session"));
            Mockito.verifyNoInteractions(response);
        }
    }
}
