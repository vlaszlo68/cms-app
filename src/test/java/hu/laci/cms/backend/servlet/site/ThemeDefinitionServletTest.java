package hu.laci.cms.backend.servlet.site;

import hu.laci.cms.backend.config.session.AppSessionManager;
import hu.laci.cms.backend.dto.auth.AuthenticatedUser;
import hu.laci.cms.backend.model.user.UserRole;
import hu.laci.cms.backend.servlet.support.ServletTestSupport;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests for the protected Site theme-definition endpoint. */
class ThemeDefinitionServletTest {

    @Test
    void mapsExactlyTheDedicatedAdminThemeDefinitionsUrl() {
        WebServlet mapping = ThemeDefinitionServlet.class.getAnnotation(WebServlet.class);

        assertNotNull(mapping);
        assertEquals(1, mapping.value().length);
        assertEquals("/api/admin/theme-definitions", mapping.value()[0]);
    }

    @Test
    void administratorReceivesExactlyTheTrustedThemeDefinitions() throws Exception {
        ThemeDefinitionServlet servlet = new ThemeDefinitionServlet();
        HttpServletRequest request = ServletTestSupport.request().build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();

        ServletTestSupport.runAsAuthenticatedUser(request, response, admin(),
                () -> servlet.doGet(request, response.build()));

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertTrue(response.getBody().contains("STANDARD"));
        assertTrue(response.getBody().contains("MODERN"));
        assertTrue(response.getBody().contains("DARK"));
        assertTrue(response.getBody().contains("CORPORATE"));
        assertTrue(response.getBody().contains("MINIMAL"));
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        ThemeDefinitionServlet servlet = new ThemeDefinitionServlet();
        HttpServletRequest request = ServletTestSupport.request().build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();
        try (MockedStatic<AppSessionManager> sessions = Mockito.mockStatic(AppSessionManager.class)) {
            sessions.when(() -> AppSessionManager.getAuthenticatedUser(request, response.build()))
                    .thenReturn(Optional.empty());

            servlet.doGet(request, response.build());
        }

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertTrue(response.getBody().contains("AUTH_REQUIRED"));
    }

    private static AuthenticatedUser admin() {
        return new AuthenticatedUser(1L, "admin", "admin@example.com", UserRole.ADMIN);
    }
}
