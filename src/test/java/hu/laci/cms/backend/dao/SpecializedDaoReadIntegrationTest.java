package hu.laci.cms.backend.dao;

import hu.laci.cms.backend.config.database.DatabaseConfig;
import hu.laci.cms.backend.config.database.migration.DatabaseMigrationRunner;
import hu.laci.cms.backend.dao.menu.MenuDaoImpl;
import hu.laci.cms.backend.dao.menu.MenuItemDaoImpl;
import hu.laci.cms.backend.dao.page.PageBlockDaoImpl;
import hu.laci.cms.backend.dao.page.PageDaoImpl;
import hu.laci.cms.backend.dao.settings.SiteSettingsDaoImpl;
import hu.laci.cms.backend.dao.template.TemplateDaoImpl;
import hu.laci.cms.backend.model.menu.Menu;
import hu.laci.cms.backend.model.menu.MenuItem;
import hu.laci.cms.backend.model.menu.MenuItemTargetType;
import hu.laci.cms.backend.model.page.Page;
import hu.laci.cms.backend.model.page.PageStatus;
import hu.laci.cms.backend.model.page.PageType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletContext;
import java.lang.reflect.Proxy;

/** Database-backed read tests for specialized DAO query methods backed by migrations. */
class SpecializedDaoReadIntegrationTest {

    @BeforeAll
    static void initializeDatabase() {
        DatabaseConfig.initialize(emptyServletContext());
        DatabaseMigrationRunner.runMigrations();
    }

    @AfterAll
    static void shutdownDatabase() {
        DatabaseConfig.shutdown();
    }

    @Test
    void specializedQueriesReadMigratedConfigurationRecords() {
        MenuDaoImpl menuDao = new MenuDaoImpl();
        MenuItemDaoImpl menuItemDao = new MenuItemDaoImpl();
        TemplateDaoImpl templateDao = new TemplateDaoImpl();
        SiteSettingsDaoImpl settingsDao = new SiteSettingsDaoImpl();
        PageBlockDaoImpl pageBlockDao = new PageBlockDaoImpl();

        Menu mainMenu = menuDao.findByCode("MAIN").orElseThrow();
        menuItemDao.findByMenuId(mainMenu.getId());
        Assertions.assertTrue(menuItemDao.findRootItems(mainMenu.getId()).stream()
                .allMatch(item -> item.getParentId() == null));
        Assertions.assertTrue(templateDao.findByCode("STANDARD").isPresent());
        Assertions.assertFalse(templateDao.findActive().isEmpty());
        Assertions.assertTrue(settingsDao.findSettings().isPresent());
        Assertions.assertTrue(pageBlockDao.findByPageId(-1L).isEmpty());
        Assertions.assertTrue(pageBlockDao.findVisibleByPageId(-1L).isEmpty());
    }

    @Test
    void publicMenuFixtureMigrationSuppliesRequiredTargetsWithoutDuplicatesAfterRerun() {
        MenuDaoImpl menuDao = new MenuDaoImpl();
        MenuItemDaoImpl menuItemDao = new MenuItemDaoImpl();
        PageDaoImpl pageDao = new PageDaoImpl();

        Menu mainMenu = menuDao.findByCode("MAIN").orElseThrow();
        Menu footerMenu = menuDao.findByCode("FOOTER").orElseThrow();
        Page home = pageDao.findBySlug("home").orElseThrow();
        Page rolunk = pageDao.findBySlug("rolunk").orElseThrow();

        Assertions.assertTrue(mainMenu.isActive());
        Assertions.assertTrue(footerMenu.isActive());
        assertPublishedContentPage(home);
        assertPublishedContentPage(rolunk);

        assertPageFixture(menuItemDao.findByMenuId(mainMenu.getId()), home.getId(), "Home", 1);
        assertPageFixture(menuItemDao.findByMenuId(mainMenu.getId()), rolunk.getId(), "Rólunk", 2);
        assertPageFixture(menuItemDao.findByMenuId(footerMenu.getId()), rolunk.getId(), "Rólunk", 1);
        assertUrlFixture(menuItemDao.findByMenuId(footerMenu.getId()), "https://example.com",
                "External Test Link", 2);

        int mainHomeTargetCount = countPageTargets(menuItemDao.findByMenuId(mainMenu.getId()), home.getId());
        int mainRolunkTargetCount = countPageTargets(menuItemDao.findByMenuId(mainMenu.getId()), rolunk.getId());
        int footerRolunkTargetCount = countPageTargets(menuItemDao.findByMenuId(footerMenu.getId()), rolunk.getId());
        int footerExternalUrlTargetCount = countUrlTargets(menuItemDao.findByMenuId(footerMenu.getId()),
                "https://example.com");

        Assertions.assertEquals(1, mainHomeTargetCount);
        Assertions.assertEquals(1, mainRolunkTargetCount);
        Assertions.assertEquals(1, footerRolunkTargetCount);
        Assertions.assertEquals(1, footerExternalUrlTargetCount);

        DatabaseMigrationRunner.runMigrations();

        Assertions.assertEquals(mainHomeTargetCount,
                countPageTargets(menuItemDao.findByMenuId(mainMenu.getId()), home.getId()));
        Assertions.assertEquals(mainRolunkTargetCount,
                countPageTargets(menuItemDao.findByMenuId(mainMenu.getId()), rolunk.getId()));
        Assertions.assertEquals(footerRolunkTargetCount,
                countPageTargets(menuItemDao.findByMenuId(footerMenu.getId()), rolunk.getId()));
        Assertions.assertEquals(footerExternalUrlTargetCount,
                countUrlTargets(menuItemDao.findByMenuId(footerMenu.getId()), "https://example.com"));
    }

    private static void assertPublishedContentPage(Page page) {
        Assertions.assertEquals(PageStatus.PUBLISHED, page.getStatus());
        Assertions.assertEquals(PageType.CONTENT, page.getPageType());
    }

    private static void assertPageFixture(java.util.List<MenuItem> items, Long pageId, String title, int sortOrder) {
        MenuItem item = items.stream()
                .filter(candidate -> candidate.getTargetType() == MenuItemTargetType.PAGE)
                .filter(candidate -> pageId.equals(candidate.getPageId()))
                .findFirst()
                .orElseThrow();
        Assertions.assertEquals(title, item.getTitle());
        Assertions.assertEquals(sortOrder, item.getSortOrder());
        Assertions.assertTrue(item.isVisible());
        Assertions.assertNull(item.getParentId());
        Assertions.assertNull(item.getTargetUrl());
    }

    private static void assertUrlFixture(java.util.List<MenuItem> items, String targetUrl, String title,
                                         int sortOrder) {
        MenuItem item = items.stream()
                .filter(candidate -> candidate.getTargetType() == MenuItemTargetType.URL)
                .filter(candidate -> targetUrl.equals(candidate.getTargetUrl()))
                .findFirst()
                .orElseThrow();
        Assertions.assertEquals(title, item.getTitle());
        Assertions.assertEquals(sortOrder, item.getSortOrder());
        Assertions.assertTrue(item.isVisible());
        Assertions.assertNull(item.getParentId());
        Assertions.assertNull(item.getPageId());
    }

    private static int countPageTargets(java.util.List<MenuItem> items, Long pageId) {
        return (int) items.stream()
                .filter(item -> item.getTargetType() == MenuItemTargetType.PAGE)
                .filter(item -> pageId.equals(item.getPageId()))
                .count();
    }

    private static int countUrlTargets(java.util.List<MenuItem> items, String targetUrl) {
        return (int) items.stream()
                .filter(item -> item.getTargetType() == MenuItemTargetType.URL)
                .filter(item -> targetUrl.equals(item.getTargetUrl()))
                .count();
    }

    private static ServletContext emptyServletContext() {
        return (ServletContext) Proxy.newProxyInstance(ServletContext.class.getClassLoader(),
                new Class<?>[]{ServletContext.class}, (proxy, method, arguments) -> {
                    if ("getInitParameter".equals(method.getName())) {
                        return null;
                    }
                    Class<?> returnType = method.getReturnType();
                    if (returnType == boolean.class) {
                        return false;
                    }
                    if (returnType == int.class) {
                        return 0;
                    }
                    return null;
                });
    }
}
