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
import hu.laci.cms.backend.service.function.GetWeatherFunction;
import hu.laci.cms.backend.service.function.SearchLocationFunction;
import hu.laci.cms.backend.service.external.cache.InMemoryTtlCache;
import hu.laci.cms.backend.service.weather.CurrentWeatherResult;
import hu.laci.cms.backend.service.weather.WeatherProviderClient;
import hu.laci.cms.backend.service.weather.WeatherResult;
import hu.laci.cms.backend.service.weather.WeatherService;
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

    @Test
    void weatherFunctionAllowsAnonymousEnabledSiteOnly() {
        Site site = new Site(1L, "site-a", "Site A", true);
        SiteApplication application = new SiteApplication(2L, site, GetWeatherFunction.APPLICATION_CODE, "Weather", true);
        SiteService siteService = Mockito.mock(SiteService.class);
        SiteApplicationService applicationService = Mockito.mock(SiteApplicationService.class);
        Mockito.when(siteService.requireActiveSite("site-a")).thenReturn(site);
        Mockito.when(applicationService.requirePublic(site, GetWeatherFunction.APPLICATION_CODE)).thenReturn(application);
        Mockito.when(applicationService.accessPolicy(application)).thenReturn(AccessPolicy.PUBLIC);
        WeatherProviderClient provider = new WeatherProviderClient() {
            @Override public java.util.List<hu.laci.cms.backend.service.weather.LocationResult> searchLocations(String query, String language) { return java.util.List.of(); }
            @Override public WeatherResult getWeather(double latitude, double longitude, String timezone) { return new WeatherResult(new CurrentWeatherResult(1, 1, 1, 1, 1), java.util.List.of()); }
        };
        GetWeatherFunction weather = new GetWeatherFunction(new com.google.gson.Gson(), new WeatherService(provider, new InMemoryTtlCache<>(), new InMemoryTtlCache<>()));
        PublicFunctionServlet dispatcher = new PublicFunctionServlet(siteService, applicationService, new FunctionRegistry(java.util.List.of(weather)), authorizationService());
        Object response = dispatcher.dispatch("site-a", GetWeatherFunction.APPLICATION_CODE, GetWeatherFunction.FUNCTION_CODE, new StringReader("{\"latitude\":47.5,\"longitude\":19.0}"));
        assertEquals("GetWeatherResponse", response.getClass().getSimpleName());
        Mockito.verify(applicationService).requirePublic(site, GetWeatherFunction.APPLICATION_CODE);
    }

    @Test
    void searchLocationFunctionAllowsAnonymousEnabledSite() {
        Site site = new Site(1L, "site-a", "Site A", true);
        SiteApplication application = new SiteApplication(2L, site, SearchLocationFunction.APPLICATION_CODE, "Weather", true);
        SiteService siteService = Mockito.mock(SiteService.class);
        SiteApplicationService applicationService = Mockito.mock(SiteApplicationService.class);
        Mockito.when(siteService.requireActiveSite("site-a")).thenReturn(site);
        Mockito.when(applicationService.requirePublic(site, SearchLocationFunction.APPLICATION_CODE)).thenReturn(application);
        Mockito.when(applicationService.accessPolicy(application)).thenReturn(AccessPolicy.PUBLIC);
        WeatherProviderClient provider = new WeatherProviderClient() {
            @Override public java.util.List<hu.laci.cms.backend.service.weather.LocationResult> searchLocations(String query, String language) {
                return java.util.List.of(new hu.laci.cms.backend.service.weather.LocationResult(1L, "Budapest", "Hungary", "HU", null, 47.5, 19.0, "Europe/Budapest"));
            }
            @Override public WeatherResult getWeather(double latitude, double longitude, String timezone) { return null; }
        };
        SearchLocationFunction search = new SearchLocationFunction(new com.google.gson.Gson(), new WeatherService(provider, new InMemoryTtlCache<>(), new InMemoryTtlCache<>()));
        PublicFunctionServlet dispatcher = new PublicFunctionServlet(siteService, applicationService, new FunctionRegistry(java.util.List.of(search)), authorizationService());
        Object response = dispatcher.dispatch("site-a", SearchLocationFunction.APPLICATION_CODE, SearchLocationFunction.FUNCTION_CODE, new StringReader("{\"query\":\"Budapest\"}"));
        assertEquals("SearchLocationResponse", response.getClass().getSimpleName());
    }

    @Test
    void weatherAvailabilityAndConcreteWrongFunctionAreRejected() {
        Site site = new Site(1L, "site-a", "Site A", true);
        SiteApplicationService applicationService = Mockito.mock(SiteApplicationService.class);
        SiteService siteService = Mockito.mock(SiteService.class);
        Mockito.when(siteService.requireActiveSite("site-a")).thenReturn(site);
        Mockito.when(applicationService.requirePublic(site, SearchLocationFunction.APPLICATION_CODE)).thenThrow(
                new hu.laci.cms.backend.service.application.SiteApplicationServiceException(SiteApplicationService.APPLICATION_NOT_FOUND, "Application not found."));
        PublicFunctionServlet unavailable = new PublicFunctionServlet(siteService, applicationService, new FunctionRegistry(java.util.List.of()), authorizationService());
        assertThrows(hu.laci.cms.backend.service.application.SiteApplicationServiceException.class,
                () -> unavailable.dispatch("site-a", SearchLocationFunction.APPLICATION_CODE, SearchLocationFunction.FUNCTION_CODE, new StringReader("{}")));

        SiteApplication active = new SiteApplication(2L, site, SearchLocationFunction.APPLICATION_CODE, "Weather", true);
        Mockito.when(applicationService.requirePublic(site, SearchLocationFunction.APPLICATION_CODE)).thenReturn(active);
        Mockito.when(applicationService.accessPolicy(active)).thenReturn(AccessPolicy.PUBLIC);
        PublicFunctionServlet wrongFunction = new PublicFunctionServlet(siteService, applicationService,
                new FunctionRegistry(java.util.List.of(new SearchLocationFunction(new com.google.gson.Gson(), new WeatherService(new EmptyWeatherProvider(), new InMemoryTtlCache<>(), new InMemoryTtlCache<>())))), authorizationService());
        assertEquals(PublicFunctionServlet.FUNCTION_NOT_FOUND, assertThrows(PublicFunctionException.class,
                () -> wrongFunction.dispatch("site-a", SearchLocationFunction.APPLICATION_CODE, GetWeatherFunction.FUNCTION_CODE, new StringReader("{}"))).getCode());
    }

    @Test
    void invalidWeatherCoordinatesAreRejectedAtDispatcherBoundary() {
        Site site = new Site(1L, "site-a", "Site A", true);
        SiteApplication application = new SiteApplication(2L, site, GetWeatherFunction.APPLICATION_CODE, "Weather", true);
        SiteService siteService = Mockito.mock(SiteService.class);
        SiteApplicationService applicationService = Mockito.mock(SiteApplicationService.class);
        Mockito.when(siteService.requireActiveSite("site-a")).thenReturn(site);
        Mockito.when(applicationService.requirePublic(site, GetWeatherFunction.APPLICATION_CODE)).thenReturn(application);
        Mockito.when(applicationService.accessPolicy(application)).thenReturn(AccessPolicy.PUBLIC);
        GetWeatherFunction weather = new GetWeatherFunction(new com.google.gson.Gson(), new WeatherService(new EmptyWeatherProvider(), new InMemoryTtlCache<>(), new InMemoryTtlCache<>()));
        PublicFunctionServlet dispatcher = new PublicFunctionServlet(siteService, applicationService, new FunctionRegistry(java.util.List.of(weather)), authorizationService());
        assertEquals(PublicFunctionServlet.INVALID_FUNCTION_INPUT, assertThrows(PublicFunctionException.class,
                () -> dispatcher.dispatch("site-a", GetWeatherFunction.APPLICATION_CODE, GetWeatherFunction.FUNCTION_CODE, new StringReader("{\"latitude\":91,\"longitude\":19}"))).getCode());
    }

    private static final class EmptyWeatherProvider implements WeatherProviderClient {
        @Override public java.util.List<hu.laci.cms.backend.service.weather.LocationResult> searchLocations(String query, String language) { return java.util.List.of(); }
        @Override public WeatherResult getWeather(double latitude, double longitude, String timezone) { return new WeatherResult(new CurrentWeatherResult(1, 1, 1, 1, 1), java.util.List.of()); }
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
