package hu.laci.cms.backend.servlet.settings;

import hu.laci.cms.backend.dto.auth.AuthenticatedUser;
import hu.laci.cms.backend.model.user.UserRole;
import hu.laci.cms.backend.service.settings.SiteSettingsService;
import hu.laci.cms.backend.servlet.support.ServletTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** Unit tests for global site-settings read and update endpoints. */
class SiteSettingsServletTest {

    @Test
    void getAndPutDispatchToSettingsServiceForAdministrator() throws Exception {
        SiteSettingsService service = Mockito.mock(SiteSettingsService.class);
        SiteSettingsServlet servlet = new SiteSettingsServlet();
        ServletTestSupport.setField(servlet, "siteSettingsService", service);
        javax.servlet.http.HttpServletRequest getRequest = ServletTestSupport.request().build();
        ServletTestSupport.ResponseFixture getResponse = ServletTestSupport.response();
        ServletTestSupport.runAsAuthenticatedUser(getRequest, getResponse, admin(), () -> servlet.doGet(getRequest, getResponse.build()));
        javax.servlet.http.HttpServletRequest putRequest = ServletTestSupport.request().withJsonBody("{}").build();
        ServletTestSupport.ResponseFixture putResponse = ServletTestSupport.response();
        ServletTestSupport.runAsAuthenticatedUser(putRequest, putResponse, admin(), () -> servlet.doPut(putRequest, putResponse.build()));

        Mockito.verify(service).getSettings();
        Mockito.verify(service).saveSettings(Mockito.any());
        Assertions.assertEquals(200, putResponse.getStatus());
    }

    @Test
    void rejectsAnonymousSettingsRequest() throws Exception {
        SiteSettingsServlet servlet = new SiteSettingsServlet();
        ServletTestSupport.setField(servlet, "siteSettingsService", Mockito.mock(SiteSettingsService.class));
        javax.servlet.http.HttpServletRequest request = ServletTestSupport.request().build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();
        try (org.mockito.MockedStatic<hu.laci.cms.backend.config.session.AppSessionManager> sessions =
                     Mockito.mockStatic(hu.laci.cms.backend.config.session.AppSessionManager.class)) {
            sessions.when(() -> hu.laci.cms.backend.config.session.AppSessionManager
                    .getAuthenticatedUser(request, response.build())).thenReturn(java.util.Optional.empty());
            servlet.doGet(request, response.build());
        }
        Assertions.assertEquals(401, response.getStatus());
    }

    private AuthenticatedUser admin() {
        return new AuthenticatedUser(1L, "admin", "admin@example.com", UserRole.ADMIN);
    }
}
