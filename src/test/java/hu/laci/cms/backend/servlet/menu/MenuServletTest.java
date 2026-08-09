package hu.laci.cms.backend.servlet.menu;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import hu.laci.cms.backend.dto.auth.AuthenticatedUser;
import hu.laci.cms.backend.dto.menu.PublicMenuItemResponse;
import hu.laci.cms.backend.model.menu.MenuItemTargetType;
import hu.laci.cms.backend.model.user.UserRole;
import hu.laci.cms.backend.service.menu.MenuItemService;
import hu.laci.cms.backend.service.menu.MenuService;
import hu.laci.cms.backend.service.menu.MenuServiceException;
import hu.laci.cms.backend.servlet.support.ServletTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Set;

/** Unit tests for menu, menu-item, and public-menu endpoint dispatch. */
class MenuServletTest {

    @Test
    void menuServletDispatchesCollectionDetailItemsAndMutations() throws Exception {
        MenuService menuService = Mockito.mock(MenuService.class);
        MenuItemService itemService = Mockito.mock(MenuItemService.class);
        MenuServlet servlet = new MenuServlet();
        ServletTestSupport.setField(servlet, "menuService", menuService);
        ServletTestSupport.setField(servlet, "menuItemService", itemService);

        invokeGet(servlet, null, menuService, itemService);
        invokeGet(servlet, "/2", menuService, itemService);
        invokeGet(servlet, "/2/items", menuService, itemService);
        invokePost(servlet, menuService);
        invokePut(servlet, "/2", menuService);
        invokeDelete(servlet, "/2", menuService);

        Mockito.verify(menuService).listMenus();
        Mockito.verify(menuService).getMenu(2L);
        Mockito.verify(itemService).listItems(2L);
        Mockito.verify(menuService).createMenu(Mockito.any());
        Mockito.verify(menuService).updateMenu(Mockito.eq(2L), Mockito.any());
        Mockito.verify(menuService).deleteMenu(2L);
    }

    @Test
    void menuItemServletDispatchesCreateUpdateAndDelete() throws Exception {
        MenuItemService service = Mockito.mock(MenuItemService.class);
        MenuItemServlet servlet = new MenuItemServlet();
        ServletTestSupport.setField(servlet, "menuItemService", service);
        javax.servlet.http.HttpServletRequest create = ServletTestSupport.request().withJsonBody("{}").build();
        ServletTestSupport.ResponseFixture createResponse = ServletTestSupport.response();
        ServletTestSupport.runAsAuthenticatedUser(create, createResponse, admin(), () -> servlet.doPost(create, createResponse.build()));
        javax.servlet.http.HttpServletRequest update = ServletTestSupport.request().withPathInfo("/2").withJsonBody("{}").build();
        ServletTestSupport.ResponseFixture updateResponse = ServletTestSupport.response();
        ServletTestSupport.runAsAuthenticatedUser(update, updateResponse, admin(), () -> servlet.doPut(update, updateResponse.build()));
        javax.servlet.http.HttpServletRequest delete = ServletTestSupport.request().withPathInfo("/2").build();
        ServletTestSupport.ResponseFixture deleteResponse = ServletTestSupport.response();
        ServletTestSupport.runAsAuthenticatedUser(delete, deleteResponse, admin(), () -> servlet.doDelete(delete, deleteResponse.build()));

        Mockito.verify(service).createItem(Mockito.any());
        Mockito.verify(service).updateItem(Mockito.eq(2L), Mockito.any());
        Mockito.verify(service).deleteItem(2L);
        Assertions.assertEquals(201, createResponse.getStatus());
    }

    @Test
    void publicMenuServletReturnsMenuAndRejectsMissingCode() throws Exception {
        MenuItemService service = Mockito.mock(MenuItemService.class);
        PublicMenuServlet servlet = new PublicMenuServlet();
        ServletTestSupport.setField(servlet, "menuItemService", service);
        javax.servlet.http.HttpServletRequest request = ServletTestSupport.request().withPathInfo("/MAIN").build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();
        servlet.doGet(request, response.build());
        Mockito.verify(service).getPublicMenu("MAIN");
        Assertions.assertEquals(200, response.getStatus());

        ServletTestSupport.ResponseFixture invalidResponse = ServletTestSupport.response();
        servlet.doGet(ServletTestSupport.request().build(), invalidResponse.build());
        Assertions.assertEquals(400, invalidResponse.getStatus());

        Mockito.doThrow(new MenuServiceException(MenuService.MENU_NOT_FOUND, "Menu not found."))
                .when(service).getPublicMenu("UNKNOWN");
        ServletTestSupport.ResponseFixture missingResponse = ServletTestSupport.response();
        servlet.doGet(ServletTestSupport.request().withPathInfo("/UNKNOWN").build(), missingResponse.build());
        Assertions.assertEquals(404, missingResponse.getStatus());
        Assertions.assertTrue(missingResponse.getBody().contains(MenuService.MENU_NOT_FOUND));
    }

    @Test
    void publicMenuServletSerializesPageAndUrlTargetsForFrontendRouting() throws Exception {
        MenuItemService service = Mockito.mock(MenuItemService.class);
        Mockito.when(service.getPublicMenu("MAIN")).thenReturn(List.of(
                new PublicMenuItemResponse(1L, "Home", MenuItemTargetType.PAGE, 11L, "home", "/", null,
                        List.of()),
                new PublicMenuItemResponse(2L, "External", MenuItemTargetType.URL, null, null, null,
                        "https://example.com", List.of())
        ));
        PublicMenuServlet servlet = new PublicMenuServlet();
        ServletTestSupport.setField(servlet, "menuItemService", service);
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();

        servlet.doGet(ServletTestSupport.request().withPathInfo("/MAIN").build(), response.build());

        JsonObject envelope = JsonParser.parseString(response.getBody()).getAsJsonObject();
        JsonArray items = envelope.getAsJsonArray("data");
        JsonObject pageItem = items.get(0).getAsJsonObject();
        JsonObject urlItem = items.get(1).getAsJsonObject();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertTrue(envelope.get("success").getAsBoolean());
        Assertions.assertEquals(Set.of("id", "title", "targetType", "pageId", "pageSlug", "path", "children"),
                pageItem.keySet());
        Assertions.assertEquals("PAGE", pageItem.get("targetType").getAsString());
        Assertions.assertEquals("home", pageItem.get("pageSlug").getAsString());
        Assertions.assertEquals("/", pageItem.get("path").getAsString());
        Assertions.assertEquals(Set.of("id", "title", "targetType", "targetUrl", "children"),
                urlItem.keySet());
        Assertions.assertEquals("URL", urlItem.get("targetType").getAsString());
        Assertions.assertEquals("https://example.com", urlItem.get("targetUrl").getAsString());
    }

    private void invokeGet(MenuServlet servlet, String path, MenuService menuService, MenuItemService itemService)
            throws Exception {
        ServletTestSupport.RequestFixture fixture = ServletTestSupport.request();
        if (path != null) {
            fixture.withPathInfo(path);
        }
        javax.servlet.http.HttpServletRequest request = fixture.build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();
        ServletTestSupport.runAsAuthenticatedUser(request, response, admin(), () -> servlet.doGet(request, response.build()));
    }

    private void invokePost(MenuServlet servlet, MenuService service) throws Exception {
        javax.servlet.http.HttpServletRequest request = ServletTestSupport.request().withJsonBody("{}").build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();
        ServletTestSupport.runAsAuthenticatedUser(request, response, admin(), () -> servlet.doPost(request, response.build()));
        Assertions.assertEquals(201, response.getStatus());
    }

    private void invokePut(MenuServlet servlet, String path, MenuService service) throws Exception {
        javax.servlet.http.HttpServletRequest request = ServletTestSupport.request().withPathInfo(path).withJsonBody("{}").build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();
        ServletTestSupport.runAsAuthenticatedUser(request, response, admin(), () -> servlet.doPut(request, response.build()));
    }

    private void invokeDelete(MenuServlet servlet, String path, MenuService service) throws Exception {
        javax.servlet.http.HttpServletRequest request = ServletTestSupport.request().withPathInfo(path).build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();
        ServletTestSupport.runAsAuthenticatedUser(request, response, admin(), () -> servlet.doDelete(request, response.build()));
    }

    private AuthenticatedUser admin() {
        return new AuthenticatedUser(1L, "admin", "admin@example.com", UserRole.ADMIN);
    }
}
