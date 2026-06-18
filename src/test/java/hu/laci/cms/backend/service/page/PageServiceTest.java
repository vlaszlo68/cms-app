package hu.laci.cms.backend.service.page;

import hu.laci.cms.backend.config.database.DatabaseConfig;
import hu.laci.cms.backend.config.database.migration.DatabaseMigrationRunner;
import hu.laci.cms.backend.config.session.SessionContext;
import hu.laci.cms.backend.dao.common.DaoRegistry;
import hu.laci.cms.backend.dao.page.PageDao;
import hu.laci.cms.backend.dao.page.PageDaoImpl;
import hu.laci.cms.backend.dao.template.TemplateDao;
import hu.laci.cms.backend.dto.page.CreatePageRequest;
import hu.laci.cms.backend.dto.page.PageListResponse;
import hu.laci.cms.backend.dto.page.PageResponse;
import hu.laci.cms.backend.dto.page.UpdatePageRequest;
import hu.laci.cms.backend.model.page.Page;
import hu.laci.cms.backend.model.page.PageStatus;
import hu.laci.cms.backend.model.page.PageType;
import hu.laci.cms.backend.model.template.Template;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageServiceTest {

    private static final String TEST_PREFIX = "page_service_test_";

    private PageDao pageDao;
    private TemplateDao templateDao;
    private PageService pageService;

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
        templateDao = DaoRegistry.getDao(Template.class);
        pageService = new PageService(pageDao, templateDao);
        deleteTestPages();
    }

    @AfterEach
    void tearDown() throws SQLException {
        SessionContext.clear();
        deleteTestPages();
    }

    @Test
    void createPagePersistsAndMapsResponse() {
        PageResponse response = pageService.createPage(createRequest("alpha", PageStatus.PUBLISHED,
                true, true));

        assertNotNull(response.getId());
        assertEquals(TEST_PREFIX + "alpha", response.getTitle());
        assertEquals(TEST_PREFIX + "alpha", response.getSlug());
        assertEquals(TEST_PREFIX + "alpha content", response.getContent());
        assertEquals(PageStatus.PUBLISHED, response.getStatus());
        assertEquals(TEST_PREFIX + "alpha meta", response.getMetaTitle());
        assertEquals(TEST_PREFIX + "alpha description", response.getMetaDescription());
        assertTrue(response.getHomepage());
        assertTrue(response.getMenuVisible());
        assertNotNull(response.getTemplateId());
        assertNotNull(response.getCreatedAt());
        assertNotNull(response.getUpdatedAt());

        Page persistedPage = pageDao.findById(response.getId()).orElseThrow();
        assertTrue(persistedPage.getHomepage());
        assertTrue(persistedPage.getMenuVisible());
        assertEquals(response.getTemplateId(), persistedPage.getTemplateId());
    }

    @Test
    void updatePageChangesPageAndFindsBySlug() {
        PageResponse createdPage = pageService.createPage(createRequest("before", PageStatus.DRAFT,
                false, true));

        PageResponse updatedPage = pageService.updatePage(createdPage.getId(),
                updateRequest("after", PageStatus.ARCHIVED, false, false));

        assertEquals(createdPage.getId(), updatedPage.getId());
        assertEquals(TEST_PREFIX + "after", updatedPage.getTitle());
        assertEquals(TEST_PREFIX + "after", updatedPage.getSlug());
        assertEquals(PageStatus.ARCHIVED, updatedPage.getStatus());
        assertFalse(updatedPage.getHomepage());
        assertFalse(updatedPage.getMenuVisible());

        PageResponse bySlug = pageService.getBySlug(TEST_PREFIX + "after");
        assertEquals(createdPage.getId(), bySlug.getId());
    }

    @Test
    void createRejectsDuplicateSlug() {
        pageService.createPage(createRequest("duplicate", PageStatus.DRAFT, false, true));

        PageServiceException exception = assertThrows(PageServiceException.class,
                () -> pageService.createPage(new CreatePageRequest(TEST_PREFIX + "other",
                        TEST_PREFIX + "duplicate", TEST_PREFIX + "other content", PageStatus.DRAFT,
                        null, null, false, true)));

        assertEquals(PageService.DUPLICATE_SLUG, exception.getCode());
    }

    @Test
    void homepagePageClearsOtherHomepageFlags() {
        PageResponse first = pageService.createPage(createRequest("home-one", PageStatus.PUBLISHED,
                true, true));
        PageResponse second = pageService.createPage(createRequest("home-two", PageStatus.PUBLISHED,
                true, true));

        assertFalse(pageDao.findById(first.getId()).orElseThrow().getHomepage());
        assertTrue(pageDao.findById(second.getId()).orElseThrow().getHomepage());
        assertEquals(second.getId(), pageService.getHomepage().getId());
    }

    @Test
    void deletePageRemovesExistingPage() {
        PageResponse createdPage = pageService.createPage(createRequest("delete", PageStatus.DRAFT,
                false, true));

        boolean deleted = pageService.deletePage(createdPage.getId());

        assertTrue(deleted);
        assertTrue(pageDao.findById(createdPage.getId()).isEmpty());
    }

    @Test
    void listPagesOrdersByTitle() {
        pageService.createPage(createRequest("bravo", PageStatus.DRAFT, false, true));
        pageService.createPage(createRequest("alpha", PageStatus.DRAFT, false, true));

        List<PageListResponse> pages = pageService.listPages().stream()
                .filter(page -> page.getSlug().startsWith(TEST_PREFIX))
                .toList();

        assertEquals(TEST_PREFIX + "alpha", pages.get(0).getTitle());
        assertEquals(TEST_PREFIX + "bravo", pages.get(1).getTitle());
    }

    @Test
    void createRejectsMissingRequiredFields() {
        PageServiceException exception = assertThrows(PageServiceException.class,
                () -> pageService.createPage(new CreatePageRequest(" ", " ", " ", null,
                        null, null, false, true)));

        assertEquals(PageService.VALIDATION_ERROR, exception.getCode());
    }

    @Test
    void createPageUsesRequestedTemplate() {
        Long templateId = templateDao.findByCode("LANDING").orElseThrow().getId();

        PageResponse response = pageService.createPage(new CreatePageRequest(
                TEST_PREFIX + "landing", TEST_PREFIX + "landing", TEST_PREFIX + "landing content",
                PageStatus.PUBLISHED, null, null, false, true, templateId));

        assertEquals(templateId, response.getTemplateId());
        assertEquals(templateId, pageDao.findById(response.getId()).orElseThrow().getTemplateId());
    }

    @Test
    void createPageRejectsUnknownTemplate() {
        PageServiceException exception = assertThrows(PageServiceException.class,
                () -> pageService.createPage(new CreatePageRequest(
                        TEST_PREFIX + "invalid-template", TEST_PREFIX + "invalid-template",
                        TEST_PREFIX + "invalid-template content", PageStatus.DRAFT,
                        null, null, false, true, Long.MAX_VALUE)));

        assertEquals(PageService.TEMPLATE_NOT_FOUND, exception.getCode());
    }

    @Test
    void blockPageAllowsMissingContent() {
        PageResponse response = pageService.createPage(new CreatePageRequest(
                TEST_PREFIX + "block", TEST_PREFIX + "block", null, PageType.BLOCK,
                PageStatus.PUBLISHED, null, null, false, true, null));

        assertEquals(PageType.BLOCK, response.getPageType());
        assertEquals(null, response.getContent());
        Page persisted = pageDao.findById(response.getId()).orElseThrow();
        assertEquals(PageType.BLOCK, persisted.getPageType());
        assertEquals(null, persisted.getContent());
    }

    @Test
    void contentPageStillRequiresContent() {
        PageServiceException exception = assertThrows(PageServiceException.class,
                () -> pageService.createPage(new CreatePageRequest(
                        TEST_PREFIX + "content-required", TEST_PREFIX + "content-required", " ",
                        PageType.CONTENT, PageStatus.DRAFT, null, null, false, true, null)));

        assertEquals(PageService.VALIDATION_ERROR, exception.getCode());
    }

    private static CreatePageRequest createRequest(String suffix, PageStatus status, Boolean homepage,
                                                   Boolean menuVisible) {
        return new CreatePageRequest(
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

    private static UpdatePageRequest updateRequest(String suffix, PageStatus status, Boolean homepage,
                                                   Boolean menuVisible) {
        return new UpdatePageRequest(
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
                PageServiceTest.class.getClassLoader(),
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
