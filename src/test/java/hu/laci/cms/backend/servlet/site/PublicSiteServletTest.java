package hu.laci.cms.backend.servlet.site;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import hu.laci.cms.backend.dto.menu.PublicMenuItemResponse;
import hu.laci.cms.backend.dto.page.PublicPageResponse;
import hu.laci.cms.backend.dto.settings.PublicSiteSettingsResponse;
import hu.laci.cms.backend.model.page.PageType;
import hu.laci.cms.backend.model.site.Site;
import hu.laci.cms.backend.service.menu.MenuItemService;
import hu.laci.cms.backend.service.page.PageService;
import hu.laci.cms.backend.service.settings.SiteSettingsService;
import hu.laci.cms.backend.service.site.SiteService;
import hu.laci.cms.backend.service.site.SiteServiceException;
import hu.laci.cms.backend.servlet.support.ServletTestSupport;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests for explicit public Site routing and inactive/unknown Site behavior. */
class PublicSiteServletTest {

    @Test
    void pageRouteResolvesActiveSiteAndPassesItToPageService() throws Exception {
        Site site = new Site(7L, "site-a", "Site A", true);
        SiteService siteService = Mockito.mock(SiteService.class);
        PageService pageService = Mockito.mock(PageService.class);
        Mockito.when(siteService.requireActiveSite("site-a")).thenReturn(site);
        Mockito.when(pageService.getPublicPageBySlug(site, "about")).thenReturn(
                new PublicPageResponse(11L, "About A", "about", PageType.CONTENT,
                        "STANDARD", "Site A content"));
        PublicSiteServlet servlet = new PublicSiteServlet();
        ServletTestSupport.setField(servlet, "siteService", siteService);
        ServletTestSupport.setField(servlet, "pageService", pageService);
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();

        servlet.doGet(ServletTestSupport.request().withPathInfo("/site-a/pages/about").build(), response.build());

        JsonObject data = JsonParser.parseString(response.getBody()).getAsJsonObject().getAsJsonObject("data");
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertEquals("Site A content", data.get("content").getAsString());
        Mockito.verify(pageService).getPublicPageBySlug(site, "about");
    }

    @Test
    void inactiveOrUnknownSiteUsesControlledNotFound() throws Exception {
        SiteService siteService = Mockito.mock(SiteService.class);
        Mockito.when(siteService.requireActiveSite("inactive")).thenThrow(
                new SiteServiceException(SiteService.SITE_NOT_FOUND, "Site not found."));
        PublicSiteServlet servlet = new PublicSiteServlet();
        ServletTestSupport.setField(servlet, "siteService", siteService);
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();

        servlet.doGet(ServletTestSupport.request().withPathInfo("/inactive/site-settings").build(), response.build());

        JsonObject error = JsonParser.parseString(response.getBody()).getAsJsonObject().getAsJsonObject("error");
        assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getStatus());
        assertEquals(SiteService.SITE_NOT_FOUND, error.get("code").getAsString());
    }

    @Test
    void menuRouteUsesTheResolvedActiveSite() throws Exception {
        Site site = new Site(7L, "site-a", "Site A", true);
        SiteService siteService = Mockito.mock(SiteService.class);
        MenuItemService menuItemService = Mockito.mock(MenuItemService.class);
        Mockito.when(siteService.requireActiveSite("site-a")).thenReturn(site);
        Mockito.when(menuItemService.getPublicMenu(site, "MAIN")).thenReturn(
                List.of(new PublicMenuItemResponse("Home A", List.of())));
        PublicSiteServlet servlet = new PublicSiteServlet();
        ServletTestSupport.setField(servlet, "siteService", siteService);
        ServletTestSupport.setField(servlet, "menuItemService", menuItemService);
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();

        servlet.doGet(ServletTestSupport.request().withPathInfo("/site-a/menus/MAIN").build(), response.build());

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertEquals("Home A", JsonParser.parseString(response.getBody()).getAsJsonObject()
                .getAsJsonArray("data").get(0).getAsJsonObject().get("title").getAsString());
        Mockito.verify(menuItemService).getPublicMenu(site, "MAIN");
    }

    @Test
    void settingsRouteUsesTheResolvedActiveSite() throws Exception {
        Site site = new Site(8L, "site-b", "Site B", true);
        SiteService siteService = Mockito.mock(SiteService.class);
        SiteSettingsService settingsService = Mockito.mock(SiteSettingsService.class);
        Mockito.when(siteService.requireActiveSite("site-b")).thenReturn(site);
        Mockito.when(settingsService.getPublicSettings(site)).thenReturn(new PublicSiteSettingsResponse(
                "Site B", null, "Footer B", null, null, null, null));
        PublicSiteServlet servlet = new PublicSiteServlet();
        ServletTestSupport.setField(servlet, "siteService", siteService);
        ServletTestSupport.setField(servlet, "siteSettingsService", settingsService);
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();

        servlet.doGet(ServletTestSupport.request().withPathInfo("/site-b/site-settings").build(), response.build());

        JsonObject data = JsonParser.parseString(response.getBody()).getAsJsonObject().getAsJsonObject("data");
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertEquals("Site B", data.get("siteName").getAsString());
        Mockito.verify(settingsService).getPublicSettings(site);
    }

    @Test
    void missingSettingsRouteIncludesEveryNullValuedPublicField() throws Exception {
        Site site = new Site(9L, "site-c", "Site C", true);
        SiteService siteService = Mockito.mock(SiteService.class);
        SiteSettingsService settingsService = Mockito.mock(SiteSettingsService.class);
        Mockito.when(siteService.requireActiveSite("site-c")).thenReturn(site);
        Mockito.when(settingsService.getPublicSettings(site)).thenReturn(new PublicSiteSettingsResponse(
                null, null, null, null, null, null, null));
        PublicSiteServlet servlet = new PublicSiteServlet();
        ServletTestSupport.setField(servlet, "siteService", siteService);
        ServletTestSupport.setField(servlet, "siteSettingsService", settingsService);
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();

        servlet.doGet(ServletTestSupport.request().withPathInfo("/site-c/site-settings").build(), response.build());

        JsonObject data = JsonParser.parseString(response.getBody()).getAsJsonObject().getAsJsonObject("data");
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertEquals(Set.of("siteName", "logoMediaId", "footerText", "contactEmail", "phone",
                "facebookUrl", "linkedinUrl"), data.keySet());
        data.entrySet().forEach(entry -> assertTrue(entry.getValue().isJsonNull(), entry.getKey()));
    }
}
