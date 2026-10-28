package hu.laci.cms.backend.servlet.site;

import com.google.gson.JsonObject;
import hu.laci.cms.backend.model.application.Application;
import hu.laci.cms.backend.model.site.Site;
import hu.laci.cms.backend.service.application.ApplicationService;
import hu.laci.cms.backend.service.function.FunctionContext;
import hu.laci.cms.backend.service.function.AccessPolicy;
import hu.laci.cms.backend.service.function.FunctionAuthorizationService;
import hu.laci.cms.backend.service.function.FunctionRegistry;
import hu.laci.cms.backend.service.function.PublicFunction;
import hu.laci.cms.backend.service.function.PublicFunctionException;
import hu.laci.cms.backend.service.site.SiteService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.StringReader;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Contract tests for the generic public-function dispatcher. */
class PublicFunctionServletTest {

    @Test
    void dispatchesOnlyRegisteredFunctionWithResolvedContext() {
        Site site = new Site(1L, "site-a", "Site A", true);
        Application application = new Application(2L, site, "TEST_APP", "Test", true);
        SiteService siteService = Mockito.mock(SiteService.class);
        ApplicationService applicationService = Mockito.mock(ApplicationService.class);
        Mockito.when(siteService.requireActiveSite("site-a")).thenReturn(site);
        Mockito.when(applicationService.requirePublic(site, "TEST_APP")).thenReturn(application);
        PublicFunction handler = new PublicFunction() {
            @Override public String getApplicationCode() { return "TEST_APP"; }
            @Override public String getFunctionCode() { return "ECHO"; }
            @Override public Object execute(FunctionContext context, JsonObject payload) {
                assertEquals(site, context.site());
                assertEquals(application, context.application());
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
        Application application = new Application(2L, site, "APP_A", "A", true);
        SiteService siteService = Mockito.mock(SiteService.class);
        ApplicationService applicationService = Mockito.mock(ApplicationService.class);
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
        Application application = new Application(2L, site, "APP_A", "A", true);
        SiteService siteService = Mockito.mock(SiteService.class);
        ApplicationService applicationService = Mockito.mock(ApplicationService.class);
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
        Application application = new Application(2L, site, "APP_A", "A", true);
        SiteService siteService = Mockito.mock(SiteService.class);
        ApplicationService applicationService = Mockito.mock(ApplicationService.class);
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

    private FunctionAuthorizationService authorizationService() {
        FunctionAuthorizationService authorizationService = Mockito.mock(FunctionAuthorizationService.class);
        Mockito.when(authorizationService.authorize(Mockito.eq(AccessPolicy.PUBLIC), Mockito.any()))
                .thenAnswer(invocation -> invocation.getArgument(1, Optional.class));
        return authorizationService;
    }
}
