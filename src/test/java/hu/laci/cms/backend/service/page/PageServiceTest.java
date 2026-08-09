package hu.laci.cms.backend.service.page;

import hu.laci.cms.backend.config.database.DatabaseConfig;
import hu.laci.cms.backend.config.database.migration.DatabaseMigrationRunner;
import hu.laci.cms.backend.config.session.SessionContext;
import hu.laci.cms.backend.dao.common.DaoRegistry;
import hu.laci.cms.backend.dao.page.PageBlockDao;
import hu.laci.cms.backend.dao.page.PageDao;
import hu.laci.cms.backend.dao.page.PageDaoImpl;
import hu.laci.cms.backend.dao.template.TemplateDao;
import hu.laci.cms.backend.dto.page.CreatePageRequest;
import hu.laci.cms.backend.dto.page.PageListResponse;
import hu.laci.cms.backend.dto.page.PageResponse;
import hu.laci.cms.backend.dto.page.PublicPageBlockResponse;
import hu.laci.cms.backend.dto.page.PublicPageResponse;
import hu.laci.cms.backend.dto.page.UpdatePageRequest;
import hu.laci.cms.backend.model.page.Page;
import hu.laci.cms.backend.model.page.PageBlock;
import hu.laci.cms.backend.model.page.PageStatus;
import hu.laci.cms.backend.model.page.PageType;
import hu.laci.cms.backend.model.template.Template;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.servlet.ServletContext;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageServiceTest {

    private static final String TEST_PREFIX = "page_service_test_";

    private PageDao pageDao;
    private PageBlockDao pageBlockDao;
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
        pageBlockDao = DaoRegistry.getDao(PageBlock.class);
        templateDao = DaoRegistry.getDao(Template.class);
        pageService = new PageService(pageDao, templateDao, pageBlockDao);
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

    @Test
    void publicPageReturnsPublishedContentWithTemplateCode() {
        Long templateId = templateDao.findByCode("LANDING").orElseThrow().getId();
        PageResponse created = pageService.createPage(new CreatePageRequest(
                TEST_PREFIX + "public", TEST_PREFIX + "public", TEST_PREFIX + "public content",
                PageType.CONTENT, PageStatus.PUBLISHED, null, null, false, true, templateId));

        PublicPageResponse response = pageService.getPublicPageBySlug(created.getSlug());

        assertEquals(created.getId(), response.getId());
        assertEquals(created.getTitle(), response.getTitle());
        assertEquals(created.getSlug(), response.getSlug());
        assertEquals(PageType.CONTENT, response.getPageType());
        assertEquals("LANDING", response.getTemplateCode());
        assertEquals(created.getContent(), response.getContent());
    }

    @Test
    void publicContentPageDoesNotQueryBlocks() {
        PageDao contentPageDao = Mockito.mock(PageDao.class);
        TemplateDao contentTemplateDao = Mockito.mock(TemplateDao.class);
        PageBlockDao contentPageBlockDao = Mockito.mock(PageBlockDao.class);
        Page contentPage = new Page(15L, "Content", "content", "Public content", PageType.CONTENT,
                PageStatus.PUBLISHED, null, null, false, true, null);
        Mockito.when(contentPageDao.findBySlug("content")).thenReturn(java.util.Optional.of(contentPage));
        PageService contentPageService = new PageService(contentPageDao, contentTemplateDao, contentPageBlockDao);

        PublicPageResponse response = contentPageService.getPublicPageBySlug("content");

        assertEquals("Public content", response.getContent());
        assertNull(response.getBlocks());
        Mockito.verifyNoInteractions(contentPageBlockDao);
    }

    @Test
    void publicBlockPageReadsVisibleBlocksOnce() {
        PageDao blockPageDao = Mockito.mock(PageDao.class);
        TemplateDao blockTemplateDao = Mockito.mock(TemplateDao.class);
        PageBlockDao blockPageBlockDao = Mockito.mock(PageBlockDao.class);
        Page blockPage = new Page(16L, "Blocks", "blocks", null, PageType.BLOCK, PageStatus.PUBLISHED,
                null, null, false, true, null);
        PageBlock first = new PageBlock(17L, 16L, "HERO", "First", 1, true, "{\"headline\":\"Hi\"}");
        PageBlock second = new PageBlock(18L, 16L, "CTA", "Second", 2, true, "{\"label\":\"Go\"}");
        Mockito.when(blockPageDao.findBySlug("blocks")).thenReturn(java.util.Optional.of(blockPage));
        Mockito.when(blockPageBlockDao.findVisibleByPageId(16L)).thenReturn(List.of(first, second));
        PageService blockPageService = new PageService(blockPageDao, blockTemplateDao, blockPageBlockDao);

        PublicPageResponse response = blockPageService.getPublicPageBySlug("blocks");

        assertNull(response.getContent());
        assertEquals(List.of(17L, 18L), response.getBlocks().stream().map(PublicPageBlockResponse::getId).toList());
        Mockito.verify(blockPageBlockDao).findVisibleByPageId(16L);
        Mockito.verifyNoMoreInteractions(blockPageBlockDao);
    }

    @Test
    void publicPageTreatsDraftAndArchivedPagesAsNotFound() {
        PageResponse draft = pageService.createPage(new CreatePageRequest(
                TEST_PREFIX + "draft", TEST_PREFIX + "draft", TEST_PREFIX + "draft content",
                PageType.CONTENT, PageStatus.DRAFT, null, null, false, true, null));
        PageResponse archived = pageService.createPage(new CreatePageRequest(
                TEST_PREFIX + "archived", TEST_PREFIX + "archived", TEST_PREFIX + "archived content",
                PageType.CONTENT, PageStatus.ARCHIVED, null, null, false, true, null));
        PageResponse draftBlock = pageService.createPage(new CreatePageRequest(
                TEST_PREFIX + "draft-block", TEST_PREFIX + "draft-block", null,
                PageType.BLOCK, PageStatus.DRAFT, null, null, false, true, null));
        PageResponse archivedBlock = pageService.createPage(new CreatePageRequest(
                TEST_PREFIX + "archived-block", TEST_PREFIX + "archived-block", null,
                PageType.BLOCK, PageStatus.ARCHIVED, null, null, false, true, null));
        assertPublicPageNotFound(draft.getSlug());
        assertPublicPageNotFound(archived.getSlug());
        assertPublicPageNotFound(draftBlock.getSlug());
        assertPublicPageNotFound(archivedBlock.getSlug());
    }

    @Test
    void publicBlockPageReturnsOnlyVisibleOrderedBlocks() {
        PageResponse blockPage = pageService.createPage(new CreatePageRequest(
                TEST_PREFIX + "block-public", TEST_PREFIX + "block-public", null,
                PageType.BLOCK, PageStatus.PUBLISHED, null, null, false, true, null));
        PageBlock later = pageBlockDao.create(new PageBlock(null, blockPage.getId(), "CTA", "Later", 20, true,
                "{\"label\":\"Later\"}"));
        pageBlockDao.create(new PageBlock(null, blockPage.getId(), "HIDDEN", "Hidden", 10, false,
                "{\"label\":\"Hidden\"}"));
        PageBlock first = pageBlockDao.create(new PageBlock(null, blockPage.getId(), "HERO", "First", 10, true,
                "{\"headline\":\"Welcome\"}"));

        PublicPageResponse response = pageService.getPublicPageBySlug(blockPage.getSlug());

        assertEquals(PageType.BLOCK, response.getPageType());
        assertEquals(null, response.getContent());
        assertEquals(List.of(first.getId(), later.getId()), response.getBlocks().stream()
                .map(PublicPageBlockResponse::getId).toList());
        assertEquals("HERO", response.getBlocks().getFirst().getBlockType());
        assertTrue(response.getBlocks().getFirst().isVisible());
        assertEquals("{\"headline\":\"Welcome\"}", response.getBlocks().getFirst().getConfigJson());
    }

    @Test
    void publicBlockPageOmitsPersistedLegacyContent() {
        Page legacyBlockPage = pageDao.create(new Page(null, TEST_PREFIX + "legacy-block",
                TEST_PREFIX + "legacy-block", "Legacy BLOCK content", PageType.BLOCK, PageStatus.PUBLISHED,
                null, null, false, true, null));

        PublicPageResponse response = pageService.getPublicPageBySlug(legacyBlockPage.getSlug());

        assertEquals(PageType.BLOCK, response.getPageType());
        assertNull(response.getContent());
        assertEquals(List.of(), response.getBlocks());
    }

    @Test
    void publicBlockPageReturnsEmptyBlocksWhenNoneAreVisible() {
        PageResponse blockPage = pageService.createPage(new CreatePageRequest(
                TEST_PREFIX + "block-empty", TEST_PREFIX + "block-empty", null,
                PageType.BLOCK, PageStatus.PUBLISHED, null, null, false, true, null));

        PublicPageResponse response = pageService.getPublicPageBySlug(blockPage.getSlug());

        assertEquals(PageType.BLOCK, response.getPageType());
        assertEquals(List.of(), response.getBlocks());
    }

    @Test
    void publicPageLookupIsCaseSensitiveAndUnknownPagesAreNotFound() {
        PageResponse created = pageService.createPage(new CreatePageRequest(
                TEST_PREFIX + "case", TEST_PREFIX + "Case", TEST_PREFIX + "case content",
                PageType.CONTENT, PageStatus.PUBLISHED, null, null, false, true, null));

        assertPublicPageNotFound(created.getSlug().toLowerCase());
        assertPublicPageNotFound(TEST_PREFIX + "unknown");
    }

    @Test
    void publicPageMapsMissingLegacyTemplateToNullTemplateCode() {
        Page legacyPage = pageDao.create(new Page(null, TEST_PREFIX + "legacy", TEST_PREFIX + "legacy",
                TEST_PREFIX + "legacy content", PageType.CONTENT, PageStatus.PUBLISHED,
                null, null, false, true, null));

        PublicPageResponse response = pageService.getPublicPageBySlug(legacyPage.getSlug());

        assertEquals(null, response.getTemplateCode());
    }

    private void assertPublicPageNotFound(String slug) {
        PageServiceException exception = assertThrows(PageServiceException.class,
                () -> pageService.getPublicPageBySlug(slug));

        assertEquals(PageService.PAGE_NOT_FOUND, exception.getCode());
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
