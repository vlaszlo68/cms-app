package hu.laci.cms.backend.servlet.settings;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import hu.laci.cms.backend.dto.settings.PublicSiteSettingsResponse;
import hu.laci.cms.backend.service.settings.SiteSettingsService;
import hu.laci.cms.backend.servlet.support.ServletTestSupport;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests for the anonymous public site-settings endpoint. */
class PublicSiteSettingsServletTest {

    @Test
    void getReturnsPublicSettingsWithoutAuthentication() throws Exception {
        SiteSettingsService service = Mockito.mock(SiteSettingsService.class);
        Mockito.when(service.getPublicSettings()).thenReturn(new PublicSiteSettingsResponse(
                "CMS", 3L, "Footer", "contact@example.com", "+36 1 234",
                "https://facebook.example", "https://linkedin.example"));
        PublicSiteSettingsServlet servlet = new PublicSiteSettingsServlet();
        ServletTestSupport.setField(servlet, "siteSettingsService", service);
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();

        servlet.doGet(ServletTestSupport.request().build(), response.build());

        JsonObject data = JsonParser.parseString(response.getBody()).getAsJsonObject().getAsJsonObject("data");
        assertEquals(200, response.getStatus());
        assertEquals(7, data.size());
        assertEquals("CMS", data.get("siteName").getAsString());
        assertEquals(3L, data.get("logoMediaId").getAsLong());
        Mockito.verify(service).getPublicSettings();
    }

    @Test
    void getRetainsAllNullPublicSettingsFields() throws Exception {
        SiteSettingsService service = Mockito.mock(SiteSettingsService.class);
        Mockito.when(service.getPublicSettings()).thenReturn(new PublicSiteSettingsResponse(
                null, null, null, null, null, null, null));
        PublicSiteSettingsServlet servlet = new PublicSiteSettingsServlet();
        ServletTestSupport.setField(servlet, "siteSettingsService", service);
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();

        servlet.doGet(ServletTestSupport.request().build(), response.build());

        JsonObject data = JsonParser.parseString(response.getBody()).getAsJsonObject().getAsJsonObject("data");
        assertEquals(7, data.size());
        assertTrue(data.get("siteName").isJsonNull());
        assertTrue(data.get("logoMediaId").isJsonNull());
        assertTrue(data.get("footerText").isJsonNull());
        assertTrue(data.get("contactEmail").isJsonNull());
        assertTrue(data.get("phone").isJsonNull());
        assertTrue(data.get("facebookUrl").isJsonNull());
        assertTrue(data.get("linkedinUrl").isJsonNull());
    }
}
