package hu.laci.cms.backend.dao;

import hu.laci.cms.backend.config.database.DatabaseConfig;
import hu.laci.cms.backend.config.database.migration.DatabaseMigrationRunner;
import hu.laci.cms.backend.dao.menu.MenuDaoImpl;
import hu.laci.cms.backend.dao.menu.MenuItemDaoImpl;
import hu.laci.cms.backend.dao.page.PageBlockDaoImpl;
import hu.laci.cms.backend.dao.settings.SiteSettingsDaoImpl;
import hu.laci.cms.backend.dao.template.TemplateDaoImpl;
import hu.laci.cms.backend.model.menu.Menu;
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
