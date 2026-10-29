package hu.laci.cms.backend.servlet.site;

import com.google.gson.JsonObject;
import hu.laci.cms.backend.config.session.AppSession;
import hu.laci.cms.backend.config.session.AppSessionManager;
import hu.laci.cms.backend.dto.auth.AuthenticatedUser;
import hu.laci.cms.backend.dto.textanalysis.AnalyzeTextResponse;
import hu.laci.cms.backend.model.application.SiteApplication;
import hu.laci.cms.backend.model.site.Site;
import hu.laci.cms.backend.model.user.UserRole;
import hu.laci.cms.backend.servlet.support.ServletTestSupport;
import hu.laci.cms.backend.service.application.SiteApplicationService;
import hu.laci.cms.backend.service.function.FunctionContext;
import hu.laci.cms.backend.service.function.AccessPolicy;
import hu.laci.cms.backend.service.function.AnalyzeTextFunction;
import hu.laci.cms.backend.service.function.FunctionAuthorizationService;
import hu.laci.cms.backend.service.function.FunctionRegistry;
import hu.laci.cms.backend.service.function.PublicFunction;
import hu.laci.cms.backend.service.function.PublicFunctionException;
import hu.laci.cms.backend.service.site.SiteService;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.StringReader;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Contract tests for the generic public-function dispatcher. */
class PublicFunctionServletTest {

    @Test
    void dispatchesOnlyRegisteredFunctionWithResolvedContext() {
        Site site = new Site(1L, "site-a", "Site A", true);
        SiteApplication application = new SiteApplication(2L, site, "TEST_APP", "Test", true);
        SiteService siteService = Mockito.mock(SiteService.class);
        SiteApplicationService applicationService = Mockito.mock(SiteApplicationService.class);
        Mockito.when(siteService.requireActiveSite("site-a")).thenReturn(site);
        Mockito.when(applicationService.requirePublic(site, "TEST_APP")).thenReturn(application);
        PublicFunction handler = new PublicFunction() {
            @Override public String getApplicationCode() { return "TEST_APP"; }
            @Override public String getFunctionCode() { return "ECHO"; }
            @Override public Object execute(FunctionContext context, JsonObject payload) {
                assertEquals(site, context.site());
                assertEquals(application, context.siteApplication());
                return payload.get("value").getAsString();
            }
        };
        PublicFunctionServlet dispatcher = new PublicFunctionServlet(siteService, applicationService,
                new FunctionRegistry(List.of(handler)), authorizationService());

        assertEquals("ok", dispatcher.dispatch("site-a", "TEST_APP", "ECHO", new StringReader("{\"value\":\"ok\"}")));
    }

    @Test
    void rejectsFunctionRegisteredForAnotherApplication() {
        Site site = new Site(1L, "site-a", "Site A", true);
        SiteApplication application = new SiteApplication(2L, site, "APP_A", "A", true);
        SiteService siteService = Mockito.mock(SiteService.class);
        SiteApplicationService applicationService = Mockito.mock(SiteApplicationService.class);
        Mockito.when(siteService.requireActiveSite("site-a")).thenReturn(site);
        Mockito.when(applicationService.requirePublic(site, "APP_A")).thenReturn(application);
        PublicFunction onlyOtherApplication = new PublicFunction() {
            @Override public String getApplicationCode() { return "APP_B"; }
            @Override public String getFunctionCode() { return "ECHO"; }
            @Override public Object execute(FunctionContext context, JsonObject payload) { return null; }
        };
        PublicFunctionServlet dispatcher = new PublicFunctionServlet(siteService, applicationService,
                new FunctionRegistry(List.of(onlyOtherApplication)), authorizationService());

        PublicFunctionException unknown = assertThrows(PublicFunctionException.class,
                () -> dispatcher.dispatch("site-a", "APP_A", "ECHO", new StringReader("{}")));
        assertEquals(PublicFunctionServlet.FUNCTION_NOT_FOUND, unknown.getCode());
    }

    @Test
    void rejectsUnknownFunctionForTheSelectedApplication() {
        Site site = new Site(1L, "site-a", "Site A", true);
        SiteApplication application = new SiteApplication(2L, site, "APP_A", "A", true);
        SiteService siteService = Mockito.mock(SiteService.class);
        SiteApplicationService applicationService = Mockito.mock(SiteApplicationService.class);
        Mockito.when(siteService.requireActiveSite("site-a")).thenReturn(site);
        Mockito.when(applicationService.requirePublic(site, "APP_A")).thenReturn(application);
        PublicFunction knownFunction = new PublicFunction() {
            @Override public String getApplicationCode() { return "APP_A"; }
            @Override public String getFunctionCode() { return "KNOWN"; }
            @Override public Object execute(FunctionContext context, JsonObject payload) { return null; }
        };
        PublicFunctionServlet dispatcher = new PublicFunctionServlet(siteService, applicationService,
                new FunctionRegistry(List.of(knownFunction)), authorizationService());

        PublicFunctionException unknown = assertThrows(PublicFunctionException.class,
                () -> dispatcher.dispatch("site-a", "APP_A", "UNKNOWN", new StringReader("{}")));

        assertEquals(PublicFunctionServlet.FUNCTION_NOT_FOUND, unknown.getCode());
    }

    @Test
    void rejectsMalformedAndNonObjectPayloadBeforeHandlerExecution() {
        Site site = new Site(1L, "site-a", "Site A", true);
        SiteApplication application = new SiteApplication(2L, site, "APP_A", "A", true);
        SiteService siteService = Mockito.mock(SiteService.class);
        SiteApplicationService applicationService = Mockito.mock(SiteApplicationService.class);
        Mockito.when(siteService.requireActiveSite("site-a")).thenReturn(site);
        Mockito.when(applicationService.requirePublic(site, "APP_A")).thenReturn(application);
        PublicFunction handler = new PublicFunction() {
            @Override public String getApplicationCode() { return "APP_A"; }
            @Override public String getFunctionCode() { return "ECHO"; }
            @Override public Object execute(FunctionContext context, JsonObject payload) { return payload; }
        };
        PublicFunctionServlet dispatcher = new PublicFunctionServlet(siteService, applicationService,
                new FunctionRegistry(List.of(handler)), authorizationService());

        PublicFunctionException malformed = assertThrows(PublicFunctionException.class,
                () -> dispatcher.dispatch("site-a", "APP_A", "ECHO", new StringReader("{")));
        PublicFunctionException nonObject = assertThrows(PublicFunctionException.class,
                () -> dispatcher.dispatch("site-a", "APP_A", "ECHO", new StringReader("[]")));

        assertEquals(PublicFunctionServlet.INVALID_FUNCTION_INPUT, malformed.getCode());
        assertEquals(PublicFunctionServlet.INVALID_FUNCTION_INPUT, nonObject.getCode());
    }

    @Test
    void realAnalyzerRequiresAuthenticationAndCsrfThenAllowsUserAndAdmin() {
        Site site = new Site(1L, "site-a", "Site A", true);
        SiteApplication application = new SiteApplication(2L, site, AnalyzeTextFunction.APPLICATION_CODE, "Text", true);
        SiteService siteService = Mockito.mock(SiteService.class);
        SiteApplicationService applicationService = Mockito.mock(SiteApplicationService.class);
        FunctionAuthorizationService authorizationService = authenticatedAuthorizationService();
        Mockito.when(siteService.requireActiveSite("site-a")).thenReturn(site);
        Mockito.when(applicationService.requirePublic(site, AnalyzeTextFunction.APPLICATION_CODE)).thenReturn(application);
        Mockito.when(applicationService.accessPolicy(application)).thenReturn(AccessPolicy.AUTHENTICATED);
        PublicFunctionServlet dispatcher = new PublicFunctionServlet(siteService, applicationService,
                new FunctionRegistry(List.of(new AnalyzeTextFunction())), authorizationService);

        HttpServletRequest anonymousRequest = ServletTestSupport.request().build();
        HttpServletResponse anonymousResponse = Mockito.mock(HttpServletResponse.class);
        try (MockedStatic<AppSessionManager> sessions = Mockito.mockStatic(AppSessionManager.class)) {
            sessions.when(() -> AppSessionManager.findSession(anonymousRequest, anonymousResponse)).thenReturn(Optional.empty());
            PublicFunctionException exception = assertThrows(PublicFunctionException.class,
                    () -> dispatcher.dispatch("site-a", AnalyzeTextFunction.APPLICATION_CODE, AnalyzeTextFunction.FUNCTION_CODE,
                            new StringReader("{\"text\":\"Java\"}"), anonymousRequest, anonymousResponse));
            assertEquals("AUTH_REQUIRED", exception.getCode());
        }

        assertAnalyzerExecution(dispatcher, new AuthenticatedUser(1L, "user", "user@example.com", UserRole.USER));
        assertAnalyzerExecution(dispatcher, new AuthenticatedUser(2L, "admin", "admin@example.com", UserRole.ADMIN));

        HttpServletRequest missingCsrfRequest = ServletTestSupport.request().build();
        HttpServletResponse missingCsrfResponse = Mockito.mock(HttpServletResponse.class);
        try (MockedStatic<AppSessionManager> sessions = Mockito.mockStatic(AppSessionManager.class)) {
            sessions.when(() -> AppSessionManager.findSession(missingCsrfRequest, missingCsrfResponse))
                    .thenReturn(Optional.of(session(new AuthenticatedUser(1L, "user", "user@example.com", UserRole.USER), "csrf")));
            PublicFunctionException exception = assertThrows(PublicFunctionException.class,
                    () -> dispatcher.dispatch("site-a", AnalyzeTextFunction.APPLICATION_CODE, AnalyzeTextFunction.FUNCTION_CODE,
                            new StringReader("{\"text\":\"Java\"}"), missingCsrfRequest, missingCsrfResponse));
            assertEquals("CSRF_INVALID", exception.getCode());
        }
    }

    private void assertAnalyzerExecution(PublicFunctionServlet dispatcher, AuthenticatedUser principal) {
        HttpServletRequest request = ServletTestSupport.request().withHeader("X-CSRF-Token", "csrf").build();
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        try (MockedStatic<AppSessionManager> sessions = Mockito.mockStatic(AppSessionManager.class)) {
            sessions.when(() -> AppSessionManager.findSession(request, response)).thenReturn(Optional.of(session(principal, "csrf")));
            AnalyzeTextResponse analysis = (AnalyzeTextResponse) dispatcher.dispatch("site-a", AnalyzeTextFunction.APPLICATION_CODE,
                    AnalyzeTextFunction.FUNCTION_CODE, new StringReader("{\"text\":\"Java java\"}"), request, response);
            assertEquals(2, analysis.getWordCount());
        }
    }

    private FunctionAuthorizationService authenticatedAuthorizationService() {
        FunctionAuthorizationService authorizationService = Mockito.mock(FunctionAuthorizationService.class);
        Mockito.when(authorizationService.authorize(Mockito.eq(AccessPolicy.AUTHENTICATED), Mockito.any()))
                .thenAnswer(invocation -> {
                    Optional<AuthenticatedUser> principal = invocation.getArgument(1);
                    if (principal.isEmpty()) {
                        throw new PublicFunctionException("AUTH_REQUIRED", "Authentication required.");
                    }
                    return principal;
                });
        return authorizationService;
    }

    private AppSession session(AuthenticatedUser principal, String csrfToken) {
        Instant now = Instant.now();
        return new AppSession("test-session", principal, csrfToken, now, now, now.plusSeconds(60));
    }

    private FunctionAuthorizationService authorizationService() {
        FunctionAuthorizationService authorizationService = Mockito.mock(FunctionAuthorizationService.class);
        Mockito.when(authorizationService.authorize(Mockito.eq(AccessPolicy.PUBLIC), Mockito.any()))
                .thenAnswer(invocation -> invocation.getArgument(1, Optional.class));
        return authorizationService;
    }
}
