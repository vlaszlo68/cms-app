package hu.laci.cms.backend.servlet.filter;

import hu.laci.cms.backend.config.session.AppSession;
import hu.laci.cms.backend.config.session.AppSessionManager;
import hu.laci.cms.backend.config.session.SessionContext;
import hu.laci.cms.backend.config.database.TransactionContext;
import hu.laci.cms.backend.dto.auth.AuthenticatedUser;
import hu.laci.cms.backend.model.user.UserRole;
import hu.laci.cms.backend.servlet.support.ServletTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

/**
 * Unit tests for authentication, CSRF, exception, CORS, and security-header filters.
 */
class SecurityFilterTest {

    @Test
    void authFilterAllowsPublicAuthEndpointWithoutSessionLookup() throws Exception {
        AuthFilter filter = new AuthFilter();
        HttpServletRequest request = ServletTestSupport.request().withServletPath("/api/auth/login").build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();
        FilterChain chain = ServletTestSupport.filterChain();

        filter.doFilter(request, response.build(), chain);

        Mockito.verify(chain).doFilter(request, response.build());
    }

    @Test
    void authFilterReturnsUnauthorizedForProtectedEndpointWithoutUser() throws Exception {
        AuthFilter filter = new AuthFilter();
        HttpServletRequest request = ServletTestSupport.request().withServletPath("/api/pages").build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();
        FilterChain chain = ServletTestSupport.filterChain();

        try (MockedStatic<AppSessionManager> sessions = Mockito.mockStatic(AppSessionManager.class)) {
            sessions.when(() -> AppSessionManager.getAuthenticatedUser(request, response.build()))
                    .thenReturn(Optional.empty());

            filter.doFilter(request, response.build(), chain);
        }

        Assertions.assertEquals(401, response.getStatus());
        Assertions.assertTrue(response.getBody().contains("AUTH_REQUIRED"));
        Mockito.verifyNoInteractions(chain);
    }

    @Test
    void csrfFilterAllowsLoginWithoutSession() throws Exception {
        CsrfFilter filter = new CsrfFilter();
        HttpServletRequest request = ServletTestSupport.request()
                .withMethod("POST")
                .withServletPath("/api/auth/login")
                .build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();
        FilterChain chain = ServletTestSupport.filterChain();

        filter.doFilter(request, response.build(), chain);

        Mockito.verify(chain).doFilter(request, response.build());
    }

    @Test
    void csrfFilterRejectsMissingSessionForStateChangingRequest() throws Exception {
        CsrfFilter filter = new CsrfFilter();
        HttpServletRequest request = ServletTestSupport.request()
                .withMethod("DELETE")
                .withServletPath("/api/pages/7")
                .build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();
        FilterChain chain = ServletTestSupport.filterChain();

        try (MockedStatic<AppSessionManager> sessions = Mockito.mockStatic(AppSessionManager.class)) {
            sessions.when(() -> AppSessionManager.findSession(request, response.build())).thenReturn(Optional.empty());

            filter.doFilter(request, response.build(), chain);
        }

        Assertions.assertEquals(401, response.getStatus());
        Assertions.assertTrue(response.getBody().contains("AUTH_REQUIRED"));
        Mockito.verifyNoInteractions(chain);
    }

    @Test
    void csrfFilterAllowsMatchingAuthenticatedToken() throws Exception {
        CsrfFilter filter = new CsrfFilter();
        HttpServletRequest request = ServletTestSupport.request()
                .withMethod("PUT")
                .withServletPath("/api/pages/7")
                .withHeader("X-CSRF-Token", "csrf-token")
                .build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();
        FilterChain chain = ServletTestSupport.filterChain();
        AppSession session = session("csrf-token");

        try (MockedStatic<AppSessionManager> sessions = Mockito.mockStatic(AppSessionManager.class)) {
            sessions.when(() -> AppSessionManager.findSession(request, response.build())).thenReturn(Optional.of(session));

            filter.doFilter(request, response.build(), chain);
        }

        Mockito.verify(chain).doFilter(request, response.build());
    }

    @Test
    void exceptionFilterConvertsUnhandledRuntimeExceptionToJson() throws Exception {
        ExceptionHandlingFilter filter = new ExceptionHandlingFilter();
        HttpServletRequest request = ServletTestSupport.request().build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();
        FilterChain chain = ServletTestSupport.filterChain();
        Mockito.doThrow(new IllegalStateException("broken")).when(chain).doFilter(request, response.build());
        Mockito.when(response.build().isCommitted()).thenReturn(false);

        filter.doFilter(request, response.build(), chain);

        Assertions.assertEquals(500, response.getStatus());
        Assertions.assertTrue(response.getBody().contains("INTERNAL_ERROR"));
        Mockito.verify(response.build()).resetBuffer();
    }

    @Test
    void corsFilterAnswersAllowedPreflightWithHeaders() throws Exception {
        CorsFilter filter = new CorsFilter();
        FilterConfig config = Mockito.mock(FilterConfig.class);
        Mockito.when(config.getInitParameter("allowedOrigins")).thenReturn("http://localhost:5173");
        filter.init(config);
        HttpServletRequest request = ServletTestSupport.request()
                .withMethod("OPTIONS")
                .withHeader("Origin", "http://localhost:5173")
                .build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();
        FilterChain chain = ServletTestSupport.filterChain();

        filter.doFilter(request, response.build(), chain);

        Assertions.assertEquals(204, response.getStatus());
        Assertions.assertEquals("http://localhost:5173", response.getHeader("Access-Control-Allow-Origin"));
        Assertions.assertEquals("X-Captcha-Id", response.getHeader("Access-Control-Expose-Headers"));
        Mockito.verifyNoInteractions(chain);
    }

    @Test
    void securityHeadersFilterAddsHeadersBeforeContinuing() throws Exception {
        SecurityHeadersFilter filter = new SecurityHeadersFilter();
        HttpServletRequest request = ServletTestSupport.request().build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();
        FilterChain chain = ServletTestSupport.filterChain();

        filter.doFilter(request, response.build(), chain);

        Assertions.assertEquals("no-store", response.getHeader("Cache-Control"));
        Assertions.assertEquals("DENY", response.getHeader("X-Frame-Options"));
        Assertions.assertEquals("no-referrer", response.getHeader("Referrer-Policy"));
        Mockito.verify(response.build()).setDateHeader("Expires", 0L);
        Mockito.verify(chain).doFilter(request, response.build());
    }

    @Test
    void characterEncodingFilterSetsUtf8BeforeContinuing() throws Exception {
        CharacterEncodingFilter filter = new CharacterEncodingFilter();
        ServletRequest request = Mockito.mock(ServletRequest.class);
        ServletResponse response = Mockito.mock(ServletResponse.class);
        FilterChain chain = ServletTestSupport.filterChain();

        filter.doFilter(request, response, chain);

        Mockito.verify(request).setCharacterEncoding("UTF-8");
        Mockito.verify(response).setCharacterEncoding("UTF-8");
        Mockito.verify(chain).doFilter(request, response);
    }

    @Test
    void appSessionContextFilterSetsUserDuringChainAndAlwaysClearsIt() throws Exception {
        AppSessionContextFilter filter = new AppSessionContextFilter();
        HttpServletRequest request = ServletTestSupport.request().build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();
        FilterChain chain = ServletTestSupport.filterChain();
        AuthenticatedUser user = new AuthenticatedUser(12L, "tester", "tester@example.com", UserRole.ADMIN);
        Mockito.doAnswer(invocation -> {
            Assertions.assertEquals(Optional.of(12L), SessionContext.getCurrentUserId());
            return null;
        }).when(chain).doFilter(request, response.build());

        try (MockedStatic<AppSessionManager> sessions = Mockito.mockStatic(AppSessionManager.class)) {
            sessions.when(() -> AppSessionManager.getAuthenticatedUser(request, response.build()))
                    .thenReturn(Optional.of(user));
            filter.doFilter(request, response.build(), chain);
        }

        Assertions.assertTrue(SessionContext.getCurrentUserId().isEmpty());
    }

    @Test
    void requestLoggingFilterResolvesUserEvenWhenDownstreamThrows() throws Exception {
        RequestLoggingFilter filter = new RequestLoggingFilter();
        HttpServletRequest request = ServletTestSupport.request()
                .withMethod("GET")
                .withRemoteAddress("127.0.0.1")
                .build();
        Mockito.when(request.getRequestURI()).thenReturn("/api/pages");
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();
        FilterChain chain = ServletTestSupport.filterChain();
        Mockito.doThrow(new ServletException("broken")).when(chain).doFilter(request, response.build());

        try (MockedStatic<AppSessionManager> sessions = Mockito.mockStatic(AppSessionManager.class)) {
            sessions.when(() -> AppSessionManager.getAuthenticatedUser(request, null)).thenReturn(Optional.empty());
            Assertions.assertThrows(ServletException.class, () -> filter.doFilter(request, response.build(), chain));
            sessions.verify(() -> AppSessionManager.getAuthenticatedUser(request, null));
        }
    }

    @Test
    void transactionFilterCommitsAndClosesSuccessfulRequest() throws Exception {
        TransactionFilter filter = new TransactionFilter();
        ServletRequest request = Mockito.mock(ServletRequest.class);
        ServletResponse response = Mockito.mock(ServletResponse.class);
        FilterChain chain = ServletTestSupport.filterChain();

        try (MockedStatic<TransactionContext> transactions = Mockito.mockStatic(TransactionContext.class)) {
            transactions.when(TransactionContext::isRollbackOnly).thenReturn(false);
            filter.doFilter(request, response, chain);

            transactions.verify(TransactionContext::begin);
            transactions.verify(TransactionContext::commit);
            transactions.verify(TransactionContext::close);
        }
    }

    @Test
    void transactionFilterRollsBackAndClosesFailedRequest() throws Exception {
        TransactionFilter filter = new TransactionFilter();
        ServletRequest request = Mockito.mock(ServletRequest.class);
        ServletResponse response = Mockito.mock(ServletResponse.class);
        FilterChain chain = ServletTestSupport.filterChain();
        Mockito.doThrow(new IOException("broken")).when(chain).doFilter(request, response);

        try (MockedStatic<TransactionContext> transactions = Mockito.mockStatic(TransactionContext.class)) {
            Assertions.assertThrows(IOException.class, () -> filter.doFilter(request, response, chain));
            transactions.verify(TransactionContext::begin);
            transactions.verify(TransactionContext::rollback);
            transactions.verify(TransactionContext::close);
        }
    }

    @Test
    void transactionFilterRollsBackInsteadOfCommittingRollbackOnlyRequest() throws Exception {
        TransactionFilter filter = new TransactionFilter();
        ServletRequest request = Mockito.mock(ServletRequest.class);
        ServletResponse response = Mockito.mock(ServletResponse.class);
        FilterChain chain = ServletTestSupport.filterChain();

        try (MockedStatic<TransactionContext> transactions = Mockito.mockStatic(TransactionContext.class)) {
            transactions.when(TransactionContext::isRollbackOnly).thenReturn(true);
            filter.doFilter(request, response, chain);

            transactions.verify(TransactionContext::rollback);
            transactions.verify(TransactionContext::close);
            transactions.verify(TransactionContext::commit, Mockito.never());
        }
    }

    @Test
    void transactionFilterWrapsCommitFailureAndStillCloses() throws Exception {
        TransactionFilter filter = new TransactionFilter();
        ServletRequest request = Mockito.mock(ServletRequest.class);
        ServletResponse response = Mockito.mock(ServletResponse.class);
        FilterChain chain = ServletTestSupport.filterChain();

        try (MockedStatic<TransactionContext> transactions = Mockito.mockStatic(TransactionContext.class)) {
            transactions.when(TransactionContext::isRollbackOnly).thenReturn(false);
            transactions.when(TransactionContext::commit).thenThrow(new java.sql.SQLException("commit failed"));

            Assertions.assertThrows(ServletException.class, () -> filter.doFilter(request, response, chain));

            transactions.verify(TransactionContext::commit);
            transactions.verify(TransactionContext::rollback);
            transactions.verify(TransactionContext::close);
        }
    }

    @Test
    void transactionFilterWrapsBeginFailureAndAttemptsCleanup() throws Exception {
        TransactionFilter filter = new TransactionFilter();
        ServletRequest request = Mockito.mock(ServletRequest.class);
        ServletResponse response = Mockito.mock(ServletResponse.class);
        FilterChain chain = ServletTestSupport.filterChain();

        try (MockedStatic<TransactionContext> transactions = Mockito.mockStatic(TransactionContext.class)) {
            transactions.when(TransactionContext::begin).thenThrow(new java.sql.SQLException("begin failed"));

            Assertions.assertThrows(ServletException.class, () -> filter.doFilter(request, response, chain));

            Mockito.verifyNoInteractions(chain);
            transactions.verify(TransactionContext::rollback);
            transactions.verify(TransactionContext::close);
        }
    }

    private AppSession session(String csrfToken) {
        AuthenticatedUser user = new AuthenticatedUser(1L, "tester", "tester@example.com", UserRole.ADMIN);
        Instant now = Instant.now();
        return new AppSession("session-id", user, csrfToken, now, now, now.plusSeconds(600));
    }
}
