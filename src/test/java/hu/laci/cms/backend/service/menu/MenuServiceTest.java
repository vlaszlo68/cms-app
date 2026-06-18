package hu.laci.cms.backend.service.menu;

import hu.laci.cms.backend.config.database.DatabaseConfig;
import hu.laci.cms.backend.config.database.migration.DatabaseMigrationRunner;
import hu.laci.cms.backend.config.session.SessionContext;
import hu.laci.cms.backend.dao.common.DaoRegistry;
import hu.laci.cms.backend.dao.menu.MenuDao;
import hu.laci.cms.backend.dao.menu.MenuItemDao;
import hu.laci.cms.backend.dao.page.PageDao;
import hu.laci.cms.backend.dto.menu.CreateMenuItemRequest;
import hu.laci.cms.backend.dto.menu.CreateMenuRequest;
import hu.laci.cms.backend.dto.menu.MenuItemResponse;
import hu.laci.cms.backend.dto.menu.MenuResponse;
import hu.laci.cms.backend.dto.menu.PublicMenuItemResponse;
import hu.laci.cms.backend.dto.menu.UpdateMenuItemRequest;
import hu.laci.cms.backend.model.menu.Menu;
import hu.laci.cms.backend.model.menu.MenuItem;
import hu.laci.cms.backend.model.menu.MenuItemTargetType;
import hu.laci.cms.backend.model.page.Page;
import hu.laci.cms.backend.model.page.PageStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletContext;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MenuServiceTest {

    private static final String PREFIX = "menu_service_test_";

    private MenuService menuService;
    private MenuItemService menuItemService;
    private MenuItemDao menuItemDao;
    private PageDao pageDao;

    @BeforeAll
    static void initializeDatabase() {
        DatabaseConfig.initialize(createEmptyServletContext());
        DatabaseMigrationRunner.runMigrations();
        DaoRegistry.initialize();
    }

    @AfterAll
    static void shutdownDatabase() {
        DaoRegistry.shutdown();
        DatabaseConfig.shutdown();
    }

    @BeforeEach
    void setUp() throws SQLException {
        SessionContext.clear();
        MenuDao menuDao = DaoRegistry.getDao(Menu.class);
        menuItemDao = DaoRegistry.getDao(MenuItem.class);
        pageDao = DaoRegistry.getDao(Page.class);
        menuService = new MenuService(menuDao);
        menuItemService = new MenuItemService(menuDao, menuItemDao);
        cleanTestData();
    }

    @AfterEach
    void tearDown() throws SQLException {
        SessionContext.clear();
        cleanTestData();
    }

    @Test
    void createAndFindMenuRejectsDuplicateCode() {
        MenuResponse created = menuService.createMenu(new CreateMenuRequest("Main", PREFIX + "MAIN", true));

        assertEquals(created.getId(), menuService.findByCode(PREFIX + "MAIN").getId());
        MenuServiceException exception = assertThrows(MenuServiceException.class,
                () -> menuService.createMenu(new CreateMenuRequest("Other", PREFIX + "MAIN", true)));
        assertEquals(MenuService.DUPLICATE_CODE, exception.getCode());
    }

    @Test
    void migrationCreatesDefaultMenus() {
        assertNotNull(menuService.findByCode("MAIN").getId());
        assertNotNull(menuService.findByCode("FOOTER").getId());
    }

    @Test
    void publicMenuBuildsSortedVisibleTree() {
        Long pageId = createPage("tree").getId();
        MenuResponse menu = menuService.createMenu(new CreateMenuRequest("Main", PREFIX + "TREE", true));

        MenuItemResponse second = menuItemService.createItem(
                new CreateMenuItemRequest(menu.getId(), null, pageId, "Second", 20, true));
        MenuItemResponse first = menuItemService.createItem(
                new CreateMenuItemRequest(menu.getId(), null, pageId, "First", 10, true));
        menuItemService.createItem(
                new CreateMenuItemRequest(menu.getId(), first.getId(), pageId, "Child", 0, true));
        menuItemService.createItem(
                new CreateMenuItemRequest(menu.getId(), null, pageId, "Hidden", 0, false));

        List<PublicMenuItemResponse> tree = menuItemService.getPublicMenu(PREFIX + "TREE");

        assertEquals(List.of("First", "Second"), tree.stream().map(PublicMenuItemResponse::getTitle).toList());
        assertEquals(List.of("Child"), tree.get(0).getChildren().stream()
                .map(PublicMenuItemResponse::getTitle).toList());
        assertEquals(MenuItemTargetType.PAGE, tree.get(0).getTargetType());
        assertEquals(pageId, tree.get(0).getPageId());
        assertNull(tree.get(0).getTargetUrl());
        List<MenuItemResponse> flatItems = menuItemService.listItems(menu.getId());
        assertEquals(second.getId(), flatItems.get(flatItems.size() - 1).getId());
    }

    @Test
    void urlTargetPersistsAndAppearsInPublicTree() {
        MenuResponse menu = menuService.createMenu(new CreateMenuRequest("Links", PREFIX + "LINKS", true));

        MenuItemResponse created = menuItemService.createItem(new CreateMenuItemRequest(
                menu.getId(), null, null, MenuItemTargetType.URL, " https://github.com ",
                "GitHub", 0, true));

        assertEquals(MenuItemTargetType.URL, created.getTargetType());
        assertNull(created.getPageId());
        assertEquals("https://github.com", created.getTargetUrl());

        MenuItem persisted = menuItemDao.findById(created.getId()).orElseThrow();
        assertEquals(MenuItemTargetType.URL, persisted.getTargetType());
        assertNull(persisted.getPageId());
        assertEquals("https://github.com", persisted.getTargetUrl());

        MenuItemResponse listed = menuItemService.listItems(menu.getId()).getFirst();
        assertEquals(MenuItemTargetType.URL, listed.getTargetType());
        assertNull(listed.getPageId());
        assertEquals("https://github.com", listed.getTargetUrl());

        PublicMenuItemResponse publicItem = menuItemService.getPublicMenu(PREFIX + "LINKS").getFirst();
        assertEquals(MenuItemTargetType.URL, publicItem.getTargetType());
        assertNull(publicItem.getPageId());
        assertEquals("https://github.com", publicItem.getTargetUrl());
    }

    @Test
    void pageTargetDiscardsTargetUrlAndRoundTripsThroughDao() {
        Long pageId = createPage("page-target").getId();
        MenuResponse menu = menuService.createMenu(new CreateMenuRequest("Pages", PREFIX + "PAGES", true));

        MenuItemResponse created = menuItemService.createItem(new CreateMenuItemRequest(
                menu.getId(), null, pageId, MenuItemTargetType.PAGE, "https://ignored.example",
                "Page", 0, true));

        assertEquals(MenuItemTargetType.PAGE, created.getTargetType());
        assertEquals(pageId, created.getPageId());
        assertNull(created.getTargetUrl());

        MenuItem persisted = menuItemDao.findById(created.getId()).orElseThrow();
        assertEquals(MenuItemTargetType.PAGE, persisted.getTargetType());
        assertEquals(pageId, persisted.getPageId());
        assertNull(persisted.getTargetUrl());
    }

    @Test
    void updateChangesPageTargetToUrlTarget() {
        Long pageId = createPage("page-to-url").getId();
        MenuResponse menu = menuService.createMenu(new CreateMenuRequest("Switch", PREFIX + "PAGE_URL", true));
        MenuItemResponse created = menuItemService.createItem(new CreateMenuItemRequest(
                menu.getId(), null, pageId, MenuItemTargetType.PAGE, null, "Target", 0, true));

        MenuItemResponse updated = menuItemService.updateItem(created.getId(), new UpdateMenuItemRequest(
                menu.getId(), null, pageId, MenuItemTargetType.URL, " https://example.com ",
                "Target", 0, true));

        assertEquals(MenuItemTargetType.URL, updated.getTargetType());
        assertNull(updated.getPageId());
        assertEquals("https://example.com", updated.getTargetUrl());
        MenuItem persisted = menuItemDao.findById(created.getId()).orElseThrow();
        assertNull(persisted.getPageId());
        assertEquals("https://example.com", persisted.getTargetUrl());
    }

    @Test
    void updateChangesUrlTargetToPageTarget() {
        Long pageId = createPage("url-to-page").getId();
        MenuResponse menu = menuService.createMenu(new CreateMenuRequest("Switch", PREFIX + "URL_PAGE", true));
        MenuItemResponse created = menuItemService.createItem(new CreateMenuItemRequest(
                menu.getId(), null, null, MenuItemTargetType.URL, "https://example.com",
                "Target", 0, true));

        MenuItemResponse updated = menuItemService.updateItem(created.getId(), new UpdateMenuItemRequest(
                menu.getId(), null, pageId, MenuItemTargetType.PAGE, "https://ignored.example",
                "Target", 0, true));

        assertEquals(MenuItemTargetType.PAGE, updated.getTargetType());
        assertEquals(pageId, updated.getPageId());
        assertNull(updated.getTargetUrl());
        MenuItem persisted = menuItemDao.findById(created.getId()).orElseThrow();
        assertEquals(pageId, persisted.getPageId());
        assertNull(persisted.getTargetUrl());
    }

    @Test
    void targetValidationDependsOnTargetType() {
        MenuResponse menu = menuService.createMenu(new CreateMenuRequest("Targets", PREFIX + "TARGETS", true));

        MenuServiceException missingPage = assertThrows(MenuServiceException.class,
                () -> menuItemService.createItem(new CreateMenuItemRequest(
                        menu.getId(), null, null, MenuItemTargetType.PAGE, null,
                        "Page", 0, true)));
        MenuServiceException missingUrl = assertThrows(MenuServiceException.class,
                () -> menuItemService.createItem(new CreateMenuItemRequest(
                        menu.getId(), null, null, MenuItemTargetType.URL, " ",
                        "URL", 0, true)));

        assertEquals(MenuItemService.VALIDATION_ERROR, missingPage.getCode());
        assertEquals(MenuItemService.VALIDATION_ERROR, missingUrl.getCode());
    }

    @Test
    void itemRejectsParentFromAnotherMenu() {
        Long pageId = createPage("parent").getId();
        MenuResponse firstMenu = menuService.createMenu(new CreateMenuRequest("First", PREFIX + "FIRST", true));
        MenuResponse secondMenu = menuService.createMenu(new CreateMenuRequest("Second", PREFIX + "SECOND", true));
        MenuItemResponse parent = menuItemService.createItem(
                new CreateMenuItemRequest(firstMenu.getId(), null, pageId, "Parent", 0, true));

        MenuServiceException exception = assertThrows(MenuServiceException.class,
                () -> menuItemService.createItem(
                        new CreateMenuItemRequest(secondMenu.getId(), parent.getId(), pageId, "Invalid", 0, true)));

        assertEquals(MenuItemService.INVALID_PARENT, exception.getCode());
    }

    private Page createPage(String suffix) {
        return pageDao.create(new Page(null, PREFIX + suffix, PREFIX + suffix, "content", PageStatus.PUBLISHED,
                null, null, false, true));
    }

    private static void cleanTestData() throws SQLException {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement deleteMenus = connection.prepareStatement("DELETE FROM menus WHERE code LIKE ?");
             PreparedStatement deletePages = connection.prepareStatement("DELETE FROM pages WHERE slug LIKE ?")) {
            deleteMenus.setString(1, PREFIX + "%");
            deleteMenus.executeUpdate();
            deletePages.setString(1, PREFIX + "%");
            deletePages.executeUpdate();
        }
    }

    private static ServletContext createEmptyServletContext() {
        return (ServletContext) Proxy.newProxyInstance(
                MenuServiceTest.class.getClassLoader(),
                new Class<?>[]{ServletContext.class},
                (proxy, method, args) -> {
                    if ("getInitParameter".equals(method.getName())) {
                        return null;
                    }
                    if ("toString".equals(method.getName())) {
                        return "TestServletContext";
                    }
                    return getDefaultValue(method.getReturnType());
                });
    }

    private static Object getDefaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == double.class) {
            return 0D;
        }
        if (returnType == float.class) {
            return 0F;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == char.class) {
            return (char) 0;
        }
        return null;
    }
}
