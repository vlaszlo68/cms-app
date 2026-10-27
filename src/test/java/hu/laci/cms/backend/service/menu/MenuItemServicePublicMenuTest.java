package hu.laci.cms.backend.service.menu;

import hu.laci.cms.backend.dao.menu.MenuDao;
import hu.laci.cms.backend.dao.menu.MenuItemDao;
import hu.laci.cms.backend.dao.page.PageDao;
import hu.laci.cms.backend.dao.application.ApplicationDao;
import hu.laci.cms.backend.model.application.Application;
import hu.laci.cms.backend.service.application.ApplicationDefinition;
import hu.laci.cms.backend.service.application.ApplicationDefinitionRegistry;
import hu.laci.cms.backend.dto.menu.PublicMenuItemResponse;
import hu.laci.cms.backend.dto.menu.CreateMenuItemRequest;
import hu.laci.cms.backend.model.menu.Menu;
import hu.laci.cms.backend.model.menu.MenuItem;
import hu.laci.cms.backend.model.menu.MenuItemTargetType;
import hu.laci.cms.backend.model.page.Page;
import hu.laci.cms.backend.model.page.PageStatus;
import hu.laci.cms.backend.model.page.PageType;
import hu.laci.cms.backend.model.site.Site;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Unit tests for public menu eligibility and route mapping. */
class MenuItemServicePublicMenuTest {

    private static final Site SITE = new Site(1L, "site-a", "Site A", true);

    @Test
    void publicMenuIncludesOnlyEligiblePageTargetsAndPreservesHierarchy() {
        MenuDao menuDao = Mockito.mock(MenuDao.class);
        MenuItemDao menuItemDao = Mockito.mock(MenuItemDao.class);
        PageDao pageDao = Mockito.mock(PageDao.class);
        Mockito.when(menuDao.findBySiteAndCode(SITE, "MAIN")).thenReturn(Optional.of(new Menu(10L, "Main", "MAIN", true)));
        Mockito.when(menuItemDao.findByMenuId(10L)).thenReturn(List.of(
                pageItem(1L, null, 100L, "Home", 10, true),
                urlItem(2L, 1L, "Docs", "https://example.com/docs", 20, true),
                pageItem(3L, null, 101L, "Draft", 30, true),
                urlItem(4L, 3L, "Hidden child", "https://example.com/hidden", 40, true),
                pageItem(5L, null, 102L, "Block", 50, true),
                pageItem(6L, null, 100L, "Invisible", 60, false)
        ));
        Page homepage = page(100L, "home", false);
        Mockito.when(pageDao.findPublishedContentBySiteAndIds(SITE, Set.of(100L, 101L, 102L))).thenReturn(List.of(homepage));

        MenuItemService service = new MenuItemService(menuDao, menuItemDao, pageDao);

        List<PublicMenuItemResponse> result = service.getPublicMenu(SITE, "MAIN");

        assertEquals(1, result.size());
        PublicMenuItemResponse home = result.getFirst();
        assertEquals(1L, home.getId());
        assertEquals("Home", home.getTitle());
        assertEquals(MenuItemTargetType.PAGE, home.getTargetType());
        assertEquals(100L, home.getPageId());
        assertEquals("home", home.getPageSlug());
        assertEquals("/", home.getPath());
        assertNull(home.getTargetUrl());
        assertEquals(1, home.getChildren().size());
        PublicMenuItemResponse docs = home.getChildren().getFirst();
        assertEquals(MenuItemTargetType.URL, docs.getTargetType());
        assertNull(docs.getPageId());
        assertNull(docs.getPageSlug());
        assertNull(docs.getPath());
        assertEquals("https://example.com/docs", docs.getTargetUrl());
        Mockito.verify(pageDao).findPublishedContentBySiteAndIds(SITE, Set.of(100L, 101L, 102L));
        Mockito.verifyNoMoreInteractions(pageDao);
    }

    @Test
    void publicMenuRejectsInactiveMenu() {
        MenuDao menuDao = Mockito.mock(MenuDao.class);
        MenuItemDao menuItemDao = Mockito.mock(MenuItemDao.class);
        PageDao pageDao = Mockito.mock(PageDao.class);
        Mockito.when(menuDao.findBySiteAndCode(SITE, "MAIN")).thenReturn(Optional.of(new Menu(10L, "Main", "MAIN", false)));
        MenuItemService service = new MenuItemService(menuDao, menuItemDao, pageDao);

        MenuServiceException exception = assertThrows(MenuServiceException.class,
                () -> service.getPublicMenu(SITE, "MAIN"));

        assertEquals(MenuService.MENU_NOT_FOUND, exception.getCode());
        Mockito.verifyNoInteractions(menuItemDao, pageDao);
    }

    @Test
    void invisibleParentSuppressesVisibleDescendantFromPublicTree() {
        MenuDao menuDao = Mockito.mock(MenuDao.class);
        MenuItemDao menuItemDao = Mockito.mock(MenuItemDao.class);
        PageDao pageDao = Mockito.mock(PageDao.class);
        Mockito.when(menuDao.findBySiteAndCode(SITE, "MAIN")).thenReturn(Optional.of(new Menu(10L, "Main", "MAIN", true)));
        Mockito.when(menuItemDao.findByMenuId(10L)).thenReturn(List.of(
                urlItem(1L, null, "Hidden parent", "https://example.com/hidden", 1, false),
                urlItem(2L, 1L, "Visible child", "https://example.com/visible", 2, true)
        ));
        MenuItemService service = new MenuItemService(menuDao, menuItemDao, pageDao);

        List<PublicMenuItemResponse> result = service.getPublicMenu(SITE, "MAIN");

        assertEquals(List.of(), result);
    }

    @Test
    void publicMenuMapsOnlyActiveSupportedApplicationTargetsToApplicationPath() {
        MenuDao menuDao = Mockito.mock(MenuDao.class);
        MenuItemDao menuItemDao = Mockito.mock(MenuItemDao.class);
        PageDao pageDao = Mockito.mock(PageDao.class);
        ApplicationDao applicationDao = Mockito.mock(ApplicationDao.class);
        Application active = new Application(50L, SITE, "TEST_APP", "Test", true);
        Application inactive = new Application(51L, SITE, "TEST_APP", "Inactive", false);
        MenuItem activeItem = new MenuItem(1L, 10L, null, null, MenuItemTargetType.APPLICATION, null,
                "Application", 1, true);
        activeItem.setApplication(active);
        MenuItem inactiveItem = new MenuItem(2L, 10L, null, null, MenuItemTargetType.APPLICATION, null,
                "Inactive", 2, true);
        inactiveItem.setApplication(inactive);
        Mockito.when(menuDao.findBySiteAndCode(SITE, "MAIN")).thenReturn(Optional.of(new Menu(10L, "Main", "MAIN", true)));
        Mockito.when(menuItemDao.findByMenuId(10L)).thenReturn(List.of(activeItem, inactiveItem));
        Mockito.when(pageDao.findPublishedContentBySiteAndIds(SITE, Set.of())).thenReturn(List.of());
        Mockito.when(applicationDao.findAllBySite(SITE)).thenReturn(List.of(active, inactive));
        MenuItemService service = new MenuItemService(menuDao, menuItemDao, pageDao, applicationDao,
                new ApplicationDefinitionRegistry(List.of(new ApplicationDefinition("TEST_APP"))));

        List<PublicMenuItemResponse> result = service.getPublicMenu(SITE, "MAIN");

        assertEquals(1, result.size());
        assertEquals(MenuItemTargetType.APPLICATION, result.getFirst().getTargetType());
        assertEquals("TEST_APP", result.getFirst().getApplicationCode());
        assertEquals("/app/TEST_APP", result.getFirst().getPath());
    }

    @Test
    void applicationTargetFromAnotherSiteIsRejected() {
        MenuDao menuDao = Mockito.mock(MenuDao.class);
        MenuItemDao menuItemDao = Mockito.mock(MenuItemDao.class);
        PageDao pageDao = Mockito.mock(PageDao.class);
        ApplicationDao applicationDao = Mockito.mock(ApplicationDao.class);
        Mockito.when(menuDao.findBySiteAndId(SITE, 10L)).thenReturn(Optional.of(new Menu(10L, "Main", "MAIN", true)));
        Mockito.when(applicationDao.findBySiteAndId(SITE, 99L)).thenReturn(Optional.empty());
        MenuItemService service = new MenuItemService(menuDao, menuItemDao, pageDao, applicationDao,
                new ApplicationDefinitionRegistry(List.of(new ApplicationDefinition("TEST_APP"))));
        CreateMenuItemRequest request = new CreateMenuItemRequest(10L, null, null, 99L,
                MenuItemTargetType.APPLICATION, null, "Cross Site", 0, true);

        MenuServiceException error = assertThrows(MenuServiceException.class,
                () -> service.createItem(SITE, 10L, request));

        assertEquals(MenuItemService.INVALID_APPLICATION_TARGET, error.getCode());
    }

    private static MenuItem pageItem(Long id, Long parentId, Long pageId, String title, int sortOrder,
                                     boolean visible) {
        return new MenuItem(id, 10L, parentId, pageId, MenuItemTargetType.PAGE, null, title, sortOrder, visible);
    }

    private static MenuItem urlItem(Long id, Long parentId, String title, String targetUrl, int sortOrder,
                                    boolean visible) {
        return new MenuItem(id, 10L, parentId, null, MenuItemTargetType.URL, targetUrl, title, sortOrder, visible);
    }

    private static Page page(Long id, String slug, boolean homepage) {
        return new Page(id, "Public page", slug, "Content", PageType.CONTENT, PageStatus.PUBLISHED,
                null, null, homepage, true, null);
    }
}
