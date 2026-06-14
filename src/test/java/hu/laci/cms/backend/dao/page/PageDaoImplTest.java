package hu.laci.cms.backend.dao.page;

import hu.laci.cms.backend.config.database.DatabaseConfig;
import hu.laci.cms.backend.config.database.migration.DatabaseMigrationRunner;
import hu.laci.cms.backend.config.session.SessionContext;
import hu.laci.cms.backend.dao.common.DaoRegistry;
import hu.laci.cms.backend.model.common.QuerySpec;
import hu.laci.cms.backend.model.page.Page;
import hu.laci.cms.backend.model.page.PageProperty;
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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageDaoImplTest {

    private static final String TEST_PREFIX = "page_dao_test_";

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
        pageDao = new PageDaoImpl();
        deleteTestPages();
    }

    @AfterEach
    void tearDown() throws SQLException {
        SessionContext.clear();
        deleteTestPages();
    }

    @Test
    void findByIdReturnsPage() throws SQLException {
        long pageId = insertPage("alpha", PageStatus.PUBLISHED, true, false);

        Optional<Page> page = pageDao.findById(pageId);

        assertTrue(page.isPresent());
        assertEquals(pageId, page.get().getId());
        assertEquals(TEST_PREFIX + "alpha", page.get().getTitle());
        assertEquals(TEST_PREFIX + "alpha", page.get().getSlug());
        assertEquals(TEST_PREFIX + "alpha content", page.get().getContent());
        assertEquals(PageStatus.PUBLISHED, page.get().getStatus());
        assertEquals(TEST_PREFIX + "alpha meta", page.get().getMetaTitle());
        assertEquals(TEST_PREFIX + "alpha description", page.get().getMetaDescription());
        assertTrue(page.get().getHomepage());
        assertFalse(page.get().getMenuVisible());
        assertNotNull(page.get().getCreatedAt());
        assertNotNull(page.get().getUpdatedAt());
    }

    @Test
    void findBySlugReturnsPage() throws SQLException {
        long pageId = insertPage("slug", PageStatus.DRAFT, false, true);

        Optional<Page> page = pageDao.findBySlug(TEST_PREFIX + "slug");

        assertTrue(page.isPresent());
        assertEquals(pageId, page.get().getId());
    }

    @Test
    void findHomepageReturnsHomepagePage() throws SQLException {
        insertPage("regular", PageStatus.DRAFT, false, true);
        long homepageId = insertPage("home", PageStatus.PUBLISHED, true, true);

        Optional<Page> page = pageDao.findHomepage();

        assertTrue(page.isPresent());
        assertEquals(homepageId, page.get().getId());
    }

    @Test
    void findHomepageReturnsEmptyWhenNoHomepageExists() throws SQLException {
        insertPage("regular", PageStatus.DRAFT, false, true);

        Optional<Page> page = pageDao.findHomepage();

        assertTrue(page.isEmpty());
    }

    @Test
    void createInsertsPageAndMapsBooleanAndEnumFields() {
        Page page = createPage("created", PageStatus.ARCHIVED, true, false);

        Page createdPage = pageDao.create(page);

        assertNotNull(createdPage.getId());
        Page loadedPage = pageDao.findById(createdPage.getId()).orElseThrow();
        assertEquals(PageStatus.ARCHIVED, loadedPage.getStatus());
        assertTrue(loadedPage.getHomepage());
        assertFalse(loadedPage.getMenuVisible());
    }

    @Test
    void updateModifiesExistingPageAndKeepsCreatedAuditFields() {
        Page page = pageDao.create(createPage("before", PageStatus.DRAFT, false, true));
        Timestamp originalCreatedAt = page.getCreatedAt();
        Long originalCreatedBy = page.getCreatedBy();
        SessionContext.setCurrentUserId(42L);

        page.setTitle(TEST_PREFIX + "after");
        page.setSlug(TEST_PREFIX + "after");
        page.setContent(TEST_PREFIX + "after content");
        page.setStatus(PageStatus.PUBLISHED);
        page.setMetaTitle(TEST_PREFIX + "after meta");
        page.setMetaDescription(TEST_PREFIX + "after description");
        page.setHomepage(Boolean.TRUE);
        page.setMenuVisible(Boolean.FALSE);

        Page updatedPage = pageDao.update(page);

        Page loadedPage = pageDao.findById(updatedPage.getId()).orElseThrow();
        assertEquals(TEST_PREFIX + "after", loadedPage.getTitle());
        assertEquals(TEST_PREFIX + "after", loadedPage.getSlug());
        assertEquals(PageStatus.PUBLISHED, loadedPage.getStatus());
        assertTrue(loadedPage.getHomepage());
        assertFalse(loadedPage.getMenuVisible());
        assertEquals(originalCreatedAt, loadedPage.getCreatedAt());
        assertEquals(originalCreatedBy, loadedPage.getCreatedBy());
        assertEquals(42L, loadedPage.getUpdatedBy());
    }

    @Test
    void findAllWithQuerySpecFiltersAndSorts() {
        Page first = pageDao.create(createPage("bravo", PageStatus.DRAFT, false, true));
        Page second = pageDao.create(createPage("alpha", PageStatus.PUBLISHED, false, true));

        List<Page> pages = pageDao.findAll(QuerySpec.<PageProperty>create()
                .where(PageProperty.STATUS).equalsTo(PageStatus.PUBLISHED)
                .orderBy(PageProperty.TITLE.asc()));

        assertEquals(List.of(second.getId()), pages.stream().map(Page::getId).toList());
        assertTrue(pageDao.findById(first.getId()).isPresent());
    }

    @Test
    void deleteRemovesExistingPage() {
        Page page = pageDao.create(createPage("delete", PageStatus.DRAFT, false, true));

        boolean deleted = pageDao.delete(page);

        assertTrue(deleted);
        assertTrue(pageDao.findById(page.getId()).isEmpty());
    }

    @Test
    void updateRejectsEntityWithoutId() {
        assertThrows(IllegalArgumentException.class,
                () -> pageDao.update(createPage("missing-id", PageStatus.DRAFT, false, true)));
    }

    private static Page createPage(String suffix, PageStatus status, Boolean homepage, Boolean menuVisible) {
        return new Page(
                null,
                TEST_PREFIX + suffix,
                TEST_PREFIX + suffix,
                TEST_PREFIX + suffix + " content",
                status,
                TEST_PREFIX + suffix + " meta",
                TEST_PREFIX + suffix + " description",
                homepage,
                menuVisible
        );
    }

    private static long insertPage(String suffix, PageStatus status, boolean homepage, boolean menuVisible)
            throws SQLException {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO pages (title, slug, content, status, meta_title, meta_description, homepage,
                                        menu_visible, created_at, updated_at)
                     VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                     RETURNING id
                     """)) {
            statement.setString(1, TEST_PREFIX + suffix);
            statement.setString(2, TEST_PREFIX + suffix);
            statement.setString(3, TEST_PREFIX + suffix + " content");
            statement.setString(4, status.name());
            statement.setString(5, TEST_PREFIX + suffix + " meta");
            statement.setString(6, TEST_PREFIX + suffix + " description");
            statement.setString(7, homepage ? "T" : "F");
            statement.setString(8, menuVisible ? "T" : "F");

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong("id");
            }
        }
    }

    private static void deleteTestPages() throws SQLException {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM pages WHERE slug LIKE ? OR title LIKE ?")) {
            statement.setString(1, TEST_PREFIX + "%");
            statement.setString(2, TEST_PREFIX + "%");
            statement.executeUpdate();
        }
    }

    private static ServletContext createEmptyServletContext() {
        return (ServletContext) Proxy.newProxyInstance(
                PageDaoImplTest.class.getClassLoader(),
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
