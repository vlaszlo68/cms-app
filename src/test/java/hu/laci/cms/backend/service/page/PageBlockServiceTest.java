package hu.laci.cms.backend.service.page;

import hu.laci.cms.backend.config.database.DatabaseConfig;
import hu.laci.cms.backend.config.database.migration.DatabaseMigrationRunner;
import hu.laci.cms.backend.config.session.SessionContext;
import hu.laci.cms.backend.dao.common.DaoRegistry;
import hu.laci.cms.backend.dao.page.PageBlockDao;
import hu.laci.cms.backend.dao.page.PageDao;
import hu.laci.cms.backend.dao.template.TemplateDao;
import hu.laci.cms.backend.dto.page.CreatePageBlockRequest;
import hu.laci.cms.backend.dto.page.CreatePageRequest;
import hu.laci.cms.backend.dto.page.PageBlockResponse;
import hu.laci.cms.backend.dto.page.PageResponse;
import hu.laci.cms.backend.dto.page.UpdatePageBlockRequest;
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

import javax.servlet.ServletContext;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageBlockServiceTest {

    private static final String PREFIX = "page_block_test_";

    private PageBlockDao pageBlockDao;
    private PageService pageService;
    private PageBlockService pageBlockService;

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
        PageDao pageDao = DaoRegistry.getDao(Page.class);
        pageBlockDao = DaoRegistry.getDao(PageBlock.class);
        TemplateDao templateDao = DaoRegistry.getDao(Template.class);
        pageService = new PageService(pageDao, templateDao);
        pageBlockService = new PageBlockService(pageDao, pageBlockDao);
        deleteTestPages();
    }

    @AfterEach
    void tearDown() throws SQLException {
        SessionContext.clear();
        deleteTestPages();
    }

    @Test
    void createListGetUpdateAndDeleteBlock() {
        PageResponse page = createBlockPage("crud");
        PageBlockResponse second = pageBlockService.createBlock(new CreatePageBlockRequest(
                page.getId(), "TEXT", "Second", 20, true, "{\"body\":\"text\"}"));
        PageBlockResponse first = pageBlockService.createBlock(new CreatePageBlockRequest(
                page.getId(), "HERO", " First ", 10, true,
                "{\"headline\":\"Welcome\"}"));

        List<PageBlockResponse> blocks = pageBlockService.listBlocks(page.getId());
        assertEquals(List.of(first.getId(), second.getId()), blocks.stream().map(PageBlockResponse::getId).toList());
        assertEquals("{\"headline\":\"Welcome\"}", pageBlockService.getBlock(first.getId()).getConfigJson());

        PageBlockResponse updated = pageBlockService.updateBlock(first.getId(), new UpdatePageBlockRequest(
                page.getId(), "CTA", null, 30, false, "{invalid-json-is-stored}"));
        assertEquals("CTA", updated.getBlockType());
        assertFalse(updated.isVisible());
        assertEquals("{invalid-json-is-stored}", updated.getConfigJson());

        assertTrue(pageBlockService.deleteBlock(second.getId()));
        assertTrue(pageBlockDao.findById(second.getId()).isEmpty());
    }

    @Test
    void visibleQueryExcludesHiddenBlocks() {
        PageResponse page = createBlockPage("visible");
        pageBlockService.createBlock(new CreatePageBlockRequest(
                page.getId(), "HERO", null, 0, true, null));
        pageBlockService.createBlock(new CreatePageBlockRequest(
                page.getId(), "CONTACT", null, 1, false, null));

        assertEquals(1, pageBlockService.listVisibleBlocks(page.getId()).size());
    }

    @Test
    void createRejectsMissingPageAndBlockType() {
        PageBlockServiceException missingPage = assertThrows(PageBlockServiceException.class,
                () -> pageBlockService.createBlock(
                        new CreatePageBlockRequest(Long.MAX_VALUE, "HERO", null, 0, true, null)));
        PageBlockServiceException missingType = assertThrows(PageBlockServiceException.class,
                () -> pageBlockService.createBlock(
                        new CreatePageBlockRequest(createBlockPage("validation").getId(), " ", null,
                                0, true, null)));

        assertEquals(PageBlockService.PAGE_NOT_FOUND, missingPage.getCode());
        assertEquals(PageBlockService.VALIDATION_ERROR, missingType.getCode());
    }

    @Test
    void deletingPageCascadesBlocks() {
        PageResponse page = createBlockPage("cascade");
        PageBlockResponse block = pageBlockService.createBlock(new CreatePageBlockRequest(
                page.getId(), "GALLERY", null, 0, true, null));

        pageService.deletePage(page.getId());

        assertTrue(pageBlockDao.findById(block.getId()).isEmpty());
    }

    private PageResponse createBlockPage(String suffix) {
        return pageService.createPage(new CreatePageRequest(
                PREFIX + suffix, PREFIX + suffix, null, PageType.BLOCK, PageStatus.PUBLISHED,
                null, null, false, true, null));
    }

    private static void deleteTestPages() throws SQLException {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM pages WHERE slug LIKE ?")) {
            statement.setString(1, PREFIX + "%");
            statement.executeUpdate();
        }
    }

    private static ServletContext createEmptyServletContext() {
        return (ServletContext) Proxy.newProxyInstance(
                PageBlockServiceTest.class.getClassLoader(),
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
