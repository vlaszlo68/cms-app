package hu.laci.cms.backend.servlet.page;

import hu.laci.cms.backend.config.session.AppSessionManager;
import hu.laci.cms.backend.dto.auth.AuthenticatedUser;
import hu.laci.cms.backend.dto.page.PageListResponse;
import hu.laci.cms.backend.dto.page.PageResponse;
import hu.laci.cms.backend.model.page.PageStatus;
import hu.laci.cms.backend.model.user.UserRole;
import hu.laci.cms.backend.service.page.PageBlockService;
import hu.laci.cms.backend.service.page.PageService;
import hu.laci.cms.backend.service.page.PageServiceException;
import hu.laci.cms.backend.servlet.support.ServletTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

/**
 * Unit tests for the administrator-only page servlet HTTP contract.
 */
class PageServletTest {

    @Test
    void getRejectsUnauthenticatedRequest() throws Exception {
        PageServlet servlet = servlet(Mockito.mock(PageService.class), Mockito.mock(PageBlockService.class));
        HttpServletRequest request = ServletTestSupport.request().build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();

        try (MockedStatic<AppSessionManager> sessions = Mockito.mockStatic(AppSessionManager.class)) {
            sessions.when(() -> AppSessionManager.getAuthenticatedUser(request, response.build()))
                    .thenReturn(Optional.empty());
            servlet.doGet(request, response.build());
        }

        Assertions.assertEquals(401, response.getStatus());
        Assertions.assertTrue(response.getBody().contains("AUTH_REQUIRED"));
    }

    @Test
    void getRejectsNonAdministratorRequest() throws Exception {
        PageServlet servlet = servlet(Mockito.mock(PageService.class), Mockito.mock(PageBlockService.class));
        HttpServletRequest request = ServletTestSupport.request().build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();

        try (MockedStatic<AppSessionManager> sessions = Mockito.mockStatic(AppSessionManager.class)) {
            sessions.when(() -> AppSessionManager.getAuthenticatedUser(request, response.build()))
                    .thenReturn(Optional.of(user(UserRole.USER)));
            servlet.doGet(request, response.build());
        }

        Assertions.assertEquals(403, response.getStatus());
        Assertions.assertTrue(response.getBody().contains("FORBIDDEN"));
    }

    @Test
    void getCollectionReturnsPageListForAdministrator() throws Exception {
        PageService pageService = Mockito.mock(PageService.class);
        PageBlockService pageBlockService = Mockito.mock(PageBlockService.class);
        Mockito.when(pageService.listPages()).thenReturn(List.of(new PageListResponse(5L, "About", "about",
                PageStatus.PUBLISHED, null, null, false, true, null, null)));
        PageServlet servlet = servlet(pageService, pageBlockService);
        HttpServletRequest request = ServletTestSupport.request().build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();

        withAdministrator(request, response, () -> servlet.doGet(request, response.build()));

        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertTrue(response.getBody().contains("about"));
        Mockito.verify(pageService).listPages();
    }

    @Test
    void getPageWithBlocksReturnsCombinedResponse() throws Exception {
        PageService pageService = Mockito.mock(PageService.class);
        PageBlockService pageBlockService = Mockito.mock(PageBlockService.class);
        Mockito.when(pageService.getPage(5L)).thenReturn(page());
        Mockito.when(pageBlockService.listBlocks(5L)).thenReturn(List.of());
        PageServlet servlet = servlet(pageService, pageBlockService);
        HttpServletRequest request = ServletTestSupport.request()
                .withPathInfo("/5")
                .withParameter("includeBlocks", "true")
                .build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();

        withAdministrator(request, response, () -> servlet.doGet(request, response.build()));

        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertTrue(response.getBody().contains("blocks"));
        Mockito.verify(pageService).getPage(5L);
        Mockito.verify(pageBlockService).listBlocks(5L);
    }

    @Test
    void getSlugPathUsesSlugServiceLookup() throws Exception {
        PageService pageService = Mockito.mock(PageService.class);
        Mockito.when(pageService.getBySlug("about")).thenReturn(page());
        PageServlet servlet = servlet(pageService, Mockito.mock(PageBlockService.class));
        HttpServletRequest request = ServletTestSupport.request().withPathInfo("/slug/about").build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();

        withAdministrator(request, response, () -> servlet.doGet(request, response.build()));

        Assertions.assertEquals(200, response.getStatus());
        Mockito.verify(pageService).getBySlug("about");
    }

    @Test
    void postCreatesPageAtCollectionPath() throws Exception {
        PageService pageService = Mockito.mock(PageService.class);
        Mockito.when(pageService.createPage(Mockito.any())).thenReturn(page());
        PageServlet servlet = servlet(pageService, Mockito.mock(PageBlockService.class));
        HttpServletRequest request = ServletTestSupport.request()
                .withJsonBody("{\"title\":\"About\",\"slug\":\"about\",\"content\":\"Text\"}")
                .build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();

        withAdministrator(request, response, () -> servlet.doPost(request, response.build()));

        Assertions.assertEquals(201, response.getStatus());
        Assertions.assertTrue(response.getBody().contains("about"));
        Mockito.verify(pageService).createPage(Mockito.any());
    }

    @Test
    void putRejectsNonNumericPageId() throws Exception {
        PageService pageService = Mockito.mock(PageService.class);
        PageServlet servlet = servlet(pageService, Mockito.mock(PageBlockService.class));
        HttpServletRequest request = ServletTestSupport.request()
                .withPathInfo("/not-a-number")
                .withJsonBody("{}")
                .build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();

        withAdministrator(request, response, () -> servlet.doPut(request, response.build()));

        Assertions.assertEquals(400, response.getStatus());
        Assertions.assertTrue(response.getBody().contains("INVALID_REQUEST"));
        Mockito.verifyNoInteractions(pageService);
    }

    @Test
    void deleteUsesPathIdAndReturnsServiceResult() throws Exception {
        PageService pageService = Mockito.mock(PageService.class);
        Mockito.when(pageService.deletePage(5L)).thenReturn(true);
        PageServlet servlet = servlet(pageService, Mockito.mock(PageBlockService.class));
        HttpServletRequest request = ServletTestSupport.request().withPathInfo("/5").build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();

        withAdministrator(request, response, () -> servlet.doDelete(request, response.build()));

        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertTrue(response.getBody().contains("true"));
        Mockito.verify(pageService).deletePage(5L);
    }

    @Test
    void getMapsMissingPageServiceErrorToNotFoundEnvelope() throws Exception {
        PageService pageService = Mockito.mock(PageService.class);
        Mockito.when(pageService.getPage(5L)).thenThrow(new PageServiceException(PageService.PAGE_NOT_FOUND,
                "Page not found."));
        PageServlet servlet = servlet(pageService, Mockito.mock(PageBlockService.class));
        HttpServletRequest request = ServletTestSupport.request().withPathInfo("/5").build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();

        withAdministrator(request, response, () -> servlet.doGet(request, response.build()));

        Assertions.assertEquals(404, response.getStatus());
        Assertions.assertTrue(response.getBody().contains(PageService.PAGE_NOT_FOUND));
    }

    private PageServlet servlet(PageService pageService, PageBlockService pageBlockService)
            throws ReflectiveOperationException {
        PageServlet servlet = new PageServlet();
        setField(servlet, "pageService", pageService);
        setField(servlet, "pageBlockService", pageBlockService);
        return servlet;
    }

    private void withAdministrator(HttpServletRequest request, ServletTestSupport.ResponseFixture response,
                                   ThrowingAction action) throws Exception {
        try (MockedStatic<AppSessionManager> sessions = Mockito.mockStatic(AppSessionManager.class)) {
            sessions.when(() -> AppSessionManager.getAuthenticatedUser(request, response.build()))
                    .thenReturn(Optional.of(user(UserRole.ADMIN)));
            action.run();
        }
    }

    private AuthenticatedUser user(UserRole role) {
        return new AuthenticatedUser(1L, "tester", "tester@example.com", role);
    }

    private PageResponse page() {
        return new PageResponse(5L, "About", "about", "Text", PageStatus.PUBLISHED,
                null, null, false, true, null, null);
    }

    private void setField(Object target, String fieldName, Object value) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @FunctionalInterface
    private interface ThrowingAction {

        void run() throws Exception;
    }
}
