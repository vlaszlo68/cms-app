package hu.laci.cms.backend.servlet.site;

import hu.laci.cms.backend.config.session.AppSessionManager;
import hu.laci.cms.backend.dto.auth.AuthenticatedUser;
import hu.laci.cms.backend.dto.application.SiteApplicationResponse;
import hu.laci.cms.backend.dto.page.CreatePageBlockRequest;
import hu.laci.cms.backend.dto.settings.SiteSettingsResponse;
import hu.laci.cms.backend.dto.site.SiteResponse;
import hu.laci.cms.backend.dto.site.SiteUsageResponse;
import hu.laci.cms.backend.model.site.Site;
import hu.laci.cms.backend.model.user.UserRole;
import hu.laci.cms.backend.service.media.MediaContent;
import hu.laci.cms.backend.service.media.MediaService;
import hu.laci.cms.backend.service.menu.MenuItemService;
import hu.laci.cms.backend.service.menu.MenuService;
import hu.laci.cms.backend.service.page.PageBlockService;
import hu.laci.cms.backend.service.page.PageBlockServiceException;
import hu.laci.cms.backend.service.page.PageService;
import hu.laci.cms.backend.service.page.PageServiceException;
import hu.laci.cms.backend.service.settings.SiteSettingsService;
import hu.laci.cms.backend.service.site.SiteService;
import hu.laci.cms.backend.service.site.SiteServiceException;
import hu.laci.cms.backend.service.site.SiteUsageService;
import hu.laci.cms.backend.service.application.SiteApplicationService;
import hu.laci.cms.backend.servlet.support.ServletTestSupport;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Security regression tests for the protected Site administration dispatcher. */
class AdminSiteServletTest {

    @Test
    void unauthenticatedSiteListIsRejected() throws Exception {
        AdminSiteServlet servlet = new AdminSiteServlet();
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

    @Test
    void dispatchesSiteCreateAndUpdate() throws Exception {
        SiteService siteService = Mockito.mock(SiteService.class);
        Mockito.when(siteService.createSite(Mockito.any())).thenReturn(
                new SiteResponse(1L, "site-a", "Site A", true, null, null));
        Mockito.when(siteService.updateSite(Mockito.eq("site-a"), Mockito.any())).thenReturn(
                new SiteResponse(1L, "site-renamed", "Renamed", false, null, null));
        AdminSiteServlet servlet = servletWith(siteService, null, null, null, null, null, null);

        HttpServletRequest createRequest = ServletTestSupport.request()
                .withJsonBody("{\"code\":\"site-a\",\"name\":\"Site A\",\"active\":true}").build();
        ServletTestSupport.ResponseFixture createResponse = ServletTestSupport.response();
        ServletTestSupport.runAsAuthenticatedUser(createRequest, createResponse, admin(),
                () -> servlet.doPost(createRequest, createResponse.build()));

        HttpServletRequest updateRequest = ServletTestSupport.request().withPathInfo("/site-a")
                .withJsonBody("{\"code\":\"site-renamed\",\"name\":\"Renamed\",\"active\":false}").build();
        ServletTestSupport.ResponseFixture updateResponse = ServletTestSupport.response();
        ServletTestSupport.runAsAuthenticatedUser(updateRequest, updateResponse, admin(),
                () -> servlet.doPut(updateRequest, updateResponse.build()));

        assertEquals(HttpServletResponse.SC_CREATED, createResponse.getStatus());
        assertTrue(createResponse.getBody().contains("site-a"));
        assertEquals(HttpServletResponse.SC_OK, updateResponse.getStatus());
        assertTrue(updateResponse.getBody().contains("site-renamed"));
        Mockito.verify(siteService).createSite(Mockito.any());
        Mockito.verify(siteService).updateSite(Mockito.eq("site-a"), Mockito.any());
    }

    @Test
    void dispatchesNestedOwnedResourcesAndBinaryMedia() throws Exception {
        Site site = new Site(7L, "site-a", "Site A", true);
        SiteService siteService = Mockito.mock(SiteService.class);
        PageBlockService pageBlockService = Mockito.mock(PageBlockService.class);
        MenuItemService menuItemService = Mockito.mock(MenuItemService.class);
        SiteSettingsService settingsService = Mockito.mock(SiteSettingsService.class);
        MediaService mediaService = Mockito.mock(MediaService.class);
        Mockito.when(siteService.requireSite("site-a")).thenReturn(site);
        Mockito.when(pageBlockService.listBlocks(site, 10L)).thenReturn(List.of());
        Mockito.when(menuItemService.listItems(site, 20L)).thenReturn(List.of());
        Mockito.when(settingsService.getSettings(site)).thenReturn(new SiteSettingsResponse(
                4L, "Site A", null, "Footer", null, null, null, null));
        Mockito.when(mediaService.getMediaContent(site, 30L)).thenReturn(
                new MediaContent("logo.svg", "image/svg+xml", 3L, new byte[]{1, 2, 3}));
        AdminSiteServlet servlet = servletWith(siteService, Mockito.mock(PageService.class), pageBlockService,
                Mockito.mock(MenuService.class), menuItemService, settingsService, mediaService);

        ServletTestSupport.ResponseFixture blocks = authenticatedGet(servlet, "/site-a/pages/10/blocks");
        ServletTestSupport.ResponseFixture items = authenticatedGet(servlet, "/site-a/menus/20/items");
        ServletTestSupport.ResponseFixture settings = authenticatedGet(servlet, "/site-a/site-settings");
        ServletTestSupport.ResponseFixture content = authenticatedGet(servlet, "/site-a/media/30/content");

        assertEquals(HttpServletResponse.SC_OK, blocks.getStatus());
        assertEquals(HttpServletResponse.SC_OK, items.getStatus());
        assertTrue(settings.getBody().contains("Site A"));
        assertEquals("image/svg+xml", content.getContentType());
        assertEquals(3, content.getBinaryBody().length);
        Mockito.verify(pageBlockService).listBlocks(site, 10L);
        Mockito.verify(menuItemService).listItems(site, 20L);
        Mockito.verify(settingsService).getSettings(site);
        Mockito.verify(mediaService).getMediaContent(site, 30L);
    }

    @Test
    void mapsPathBodyOwnerMismatchToValidationError() throws Exception {
        Site site = new Site(7L, "site-a", "Site A", true);
        SiteService siteService = Mockito.mock(SiteService.class);
        PageBlockService blockService = Mockito.mock(PageBlockService.class);
        Mockito.when(siteService.requireSite("site-a")).thenReturn(site);
        Mockito.when(blockService.createBlock(Mockito.eq(site), Mockito.eq(10L), Mockito.any()))
                .thenThrow(new PageBlockServiceException(PageBlockService.VALIDATION_ERROR,
                        "pageId must match the page path."));
        AdminSiteServlet servlet = servletWith(siteService, null, blockService, null, null, null, null);
        HttpServletRequest request = ServletTestSupport.request().withPathInfo("/site-a/pages/10/blocks")
                .withJsonBody("{\"pageId\":11,\"blockType\":\"TEXT\",\"sortOrder\":0,\"visible\":true}")
                .build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();

        ServletTestSupport.runAsAuthenticatedUser(request, response, admin(),
                () -> servlet.doPost(request, response.build()));

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
        assertTrue(response.getBody().contains(PageBlockService.VALIDATION_ERROR));
        Mockito.verify(blockService).createBlock(Mockito.eq(site), Mockito.eq(10L),
                Mockito.argThat(body -> Long.valueOf(11L).equals(((CreatePageBlockRequest) body).getPageId())));
    }

    @Test
    void mapsSiteScopedEntityMissToResourceNotFound() throws Exception {
        Site site = new Site(7L, "site-a", "Site A", true);
        SiteService siteService = Mockito.mock(SiteService.class);
        PageService pageService = Mockito.mock(PageService.class);
        Mockito.when(siteService.requireSite("site-a")).thenReturn(site);
        Mockito.when(pageService.getPage(site, 99L)).thenThrow(
                new PageServiceException(PageService.PAGE_NOT_FOUND, "Page not found."));
        AdminSiteServlet servlet = servletWith(siteService, pageService, null, null, null, null, null);

        ServletTestSupport.ResponseFixture response = authenticatedGet(servlet, "/site-a/pages/99");

        assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getStatus());
        assertTrue(response.getBody().contains(PageService.PAGE_NOT_FOUND));
    }

    @Test
    void authenticatedAdministratorCanListAndCreateSiteApplications() throws Exception {
        Site site = new Site(7L, "site-a", "Site A", true);
        SiteService siteService = Mockito.mock(SiteService.class);
        SiteApplicationService applicationService = Mockito.mock(SiteApplicationService.class);
        Mockito.when(siteService.requireSite("site-a")).thenReturn(site);
        Mockito.when(applicationService.list(site)).thenReturn(List.of(
                new SiteApplicationResponse(4L, "TEST_APP", "Test", true)));
        Mockito.when(applicationService.create(Mockito.eq(site), Mockito.any())).thenReturn(
                new SiteApplicationResponse(5L, "TEST_APP", "Created", true));
        AdminSiteServlet servlet = servletWith(siteService, null, null, null, null, null, null);
        ServletTestSupport.setField(servlet, "applicationService", applicationService);

        ServletTestSupport.ResponseFixture listResponse = authenticatedGet(servlet, "/site-a/applications");
        HttpServletRequest createRequest = ServletTestSupport.request()
                .withPathInfo("/site-a/applications")
                .withJsonBody("{\"code\":\"TEST_APP\",\"name\":\"Created\",\"active\":true}")
                .build();
        ServletTestSupport.ResponseFixture createResponse = ServletTestSupport.response();
        ServletTestSupport.runAsAuthenticatedUser(createRequest, createResponse, admin(),
                () -> servlet.doPost(createRequest, createResponse.build()));

        assertEquals(HttpServletResponse.SC_OK, listResponse.getStatus());
        assertTrue(listResponse.getBody().contains("TEST_APP"));
        assertEquals(HttpServletResponse.SC_CREATED, createResponse.getStatus());
        assertTrue(createResponse.getBody().contains("Created"));
        Mockito.verify(applicationService).list(site);
        Mockito.verify(applicationService).create(Mockito.eq(site), Mockito.any());
    }

    @Test
    void exposesUsageDeletesEmptySiteAndReturnsStructuredConflict() throws Exception {
        Site site = new Site(7L, "site-a", "Site A", true);
        SiteService siteService = Mockito.mock(SiteService.class);
        SiteUsageService usageService = Mockito.mock(SiteUsageService.class);
        Mockito.when(siteService.requireSite("site-a")).thenReturn(site);
        Mockito.when(usageService.getUsage(site)).thenReturn(new SiteUsageResponse(0, 0, 0, true));
        AdminSiteServlet servlet = servletWith(siteService, null, null, null, null, null, null);
        ServletTestSupport.setField(servlet, "siteUsageService", usageService);

        ServletTestSupport.ResponseFixture usageResponse = authenticatedGet(servlet, "/site-a/usage");
        HttpServletRequest deleteRequest = ServletTestSupport.request().withPathInfo("/site-a").build();
        ServletTestSupport.ResponseFixture deleteResponse = ServletTestSupport.response();
        ServletTestSupport.runAsAuthenticatedUser(deleteRequest, deleteResponse, admin(),
                () -> servlet.doDelete(deleteRequest, deleteResponse.build()));

        assertEquals(HttpServletResponse.SC_OK, usageResponse.getStatus());
        assertTrue(usageResponse.getBody().contains("deletable"));
        assertEquals(HttpServletResponse.SC_OK, deleteResponse.getStatus());
        Mockito.verify(usageService).deleteSite(site);

        SiteUsageResponse blocked = new SiteUsageResponse(1, 0, 0, false);
        Mockito.doThrow(new SiteServiceException(SiteUsageService.SITE_IN_USE, "blocked", blocked))
                .when(usageService).deleteSite(site);
        ServletTestSupport.ResponseFixture blockedResponse = ServletTestSupport.response();
        ServletTestSupport.runAsAuthenticatedUser(deleteRequest, blockedResponse, admin(),
                () -> servlet.doDelete(deleteRequest, blockedResponse.build()));
        assertEquals(HttpServletResponse.SC_CONFLICT, blockedResponse.getStatus());
        assertTrue(blockedResponse.getBody().contains("SITE_IN_USE"));
        assertTrue(blockedResponse.getBody().contains("pageCount"));
    }

    @Test
    void permitsThemeDefinitionsAsAnOrdinarySiteCode() throws Exception {
        SiteService siteService = Mockito.mock(SiteService.class);
        Mockito.when(siteService.getSite("theme-definitions")).thenReturn(
                new SiteResponse(7L, "theme-definitions", "Theme Definitions", true, "STANDARD", null, null));
        AdminSiteServlet servlet = servletWith(siteService, null, null, null, null, null, null);

        ServletTestSupport.ResponseFixture response = authenticatedGet(servlet, "/theme-definitions");

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertTrue(response.getBody().contains("Theme Definitions"));
        Mockito.verify(siteService).getSite("theme-definitions");
    }

    @Test
    private static AdminSiteServlet servletWith(SiteService siteService, PageService pageService,
                                                PageBlockService blockService, MenuService menuService,
                                                MenuItemService itemService, SiteSettingsService settingsService,
                                                MediaService mediaService) throws ReflectiveOperationException {
        AdminSiteServlet servlet = new AdminSiteServlet();
        ServletTestSupport.setField(servlet, "siteService", siteService);
        ServletTestSupport.setField(servlet, "pageService", pageService);
        ServletTestSupport.setField(servlet, "pageBlockService", blockService);
        ServletTestSupport.setField(servlet, "menuService", menuService);
        ServletTestSupport.setField(servlet, "menuItemService", itemService);
        ServletTestSupport.setField(servlet, "siteSettingsService", settingsService);
        ServletTestSupport.setField(servlet, "mediaService", mediaService);
        return servlet;
    }

    private static ServletTestSupport.ResponseFixture authenticatedGet(AdminSiteServlet servlet, String path)
            throws Exception {
        HttpServletRequest request = ServletTestSupport.request().withPathInfo(path).build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();
        ServletTestSupport.runAsAuthenticatedUser(request, response, admin(),
                () -> servlet.doGet(request, response.build()));
        return response;
    }

    private static AuthenticatedUser admin() {
        return new AuthenticatedUser(1L, "admin", "admin@example.com", UserRole.ADMIN);
    }
}
