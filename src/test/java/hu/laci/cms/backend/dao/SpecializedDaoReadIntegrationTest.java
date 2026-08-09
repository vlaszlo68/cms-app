package hu.laci.cms.backend.dao;

import com.google.gson.JsonParser;
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
import hu.laci.cms.backend.model.page.PageBlock;
import hu.laci.cms.backend.model.page.PageStatus;
import hu.laci.cms.backend.model.page.PageType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletContext;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

/** Database-backed read tests for specialized DAO query methods backed by migrations. */
class SpecializedDaoReadIntegrationTest {

    private static final String HOME_CONTENT = String.join("\n",
            "<h1>AI Coding Workflow Hub</h1>",
            "<p>AI-assisted software development works best when coding agents do not simply generate random code, but follow a structured workflow. This demo CMS site presents a development model where specialized agents help with exploration, planning, implementation, review and iterative correction.</p>",
            "<p>The goal is to show how backend, frontend-admin and frontend-public agents can collaborate on one product while still working in separate codebases and contexts. Each agent has a clear responsibility, and every larger feature is designed as a vertical slice that can be tested through a real user flow.</p>",
            "<h2>What this demo site demonstrates</h2>",
            "<p>This CMS is used to demonstrate how content, menus, templates and page blocks can be managed from an admin interface and rendered on a public website. The sample content focuses on AI coding agents, integration contracts, review-driven development and safer incremental delivery.</p>",
            "<ul><li>Structured planning before implementation</li><li>Separate backend, frontend-admin and frontend-public agent responsibilities</li><li>Review and fixer iterations before a feature is considered complete</li><li>Vertical-slice delivery instead of disconnected technical layers</li><li>Public rendering of CMS-managed pages and blocks</li></ul>",
            "<p>This page is a CONTENT page. It is rendered directly from the Page.content field and is useful for validating the basic public page rendering pipeline.</p>");
    private static final String ROLUNK_CONTENT = String.join("\n",
            "<h1>About the AI Agent Workflow</h1>",
            "<p>This demo website describes a structured software development workflow supported by AI coding agents. Instead of asking one agent to do everything at once, the work is divided into clear roles: Explorer, Planner, Implementer, Reviewer, Fixer, Final Reviewer and Summary.</p>",
            "<p>The Explorer understands the existing codebase and identifies architectural constraints. The Planner creates an implementation plan, integration contract, acceptance criteria and test strategy. The Implementer follows the plan and changes only what is needed. The Reviewer checks correctness, architecture, security, maintainability and E2E testability. If problems are found, the Fixer corrects them and the Final Reviewer verifies the result.</p>",
            "<h2>Why separate agents?</h2>",
            "<p>In a fullstack project, backend and frontend work often require different context. A backend agent can make an API backend-ready and document the integration contract. A frontend-public agent can then use that contract to complete the browser-level vertical slice. A frontend-admin agent can focus on the CMS management experience.</p>",
            "<h2>Why vertical slices?</h2>",
            "<p>A feature is most valuable when it can be tested as a real user flow. For example, a CMS page should not only exist in the database; it should be available through the public API and visible in the browser. This approach keeps development safer, more understandable and easier to review.</p>",
            "<p>This page is also a CONTENT page and should continue to be useful for validating ordinary HTML content rendering on the public frontend.</p>");
    private static final String HERO_CONFIG = "{\"headline\":\"Build software with structured AI coding agents\",\"subHeadline\":\"Plan, implement, review and improve features through a repeatable development workflow.\",\"buttonLabel\":\"Explore the workflow\",\"buttonUrl\":\"/rolunk\"}";
    private static final String TEXT_CONFIG = "{\"html\":\"<h2>From prompt to reviewed code</h2><p>This BLOCK page is built from PageBlock records. It demonstrates how a CMS page can be composed from independent sections instead of one large HTML field.</p><p>In this demo, each block represents a part of an AI-assisted development process. The HERO block introduces the main idea, the TEXT block explains the workflow, and the CTA block guides the visitor to the next step.</p><p>This structure will make it possible to build more advanced pages later, including landing pages, galleries, feature sections and media-rich public content.</p>\"}";
    private static final String CTA_CONFIG = "{\"title\":\"Ready to deliver safer features?\",\"text\":\"Use specialized backend, frontend and reviewer agents to build smaller, clearer and more testable vertical slices.\",\"buttonLabel\":\"Read about the workflow\",\"buttonUrl\":\"/rolunk\"}";
    private static final String ENRICHED_HERO_CONFIG = "{\"headline\":\"Build software with structured AI coding agents\",\"subHeadline\":\"Use specialized agents for planning, implementation, review and iterative fixing while keeping every feature small, testable and understandable.\",\"buttonLabel\":\"Learn about the workflow\",\"buttonUrl\":\"/rolunk\"}";

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

    @Test
    void aiCodingAgentDemoMigrationSuppliesExactContentAndVisibleOrderedBlocksWithoutDuplicatesAfterRerun() {
        PageDaoImpl pageDao = new PageDaoImpl();
        PageBlockDaoImpl pageBlockDao = new PageBlockDaoImpl();
        TemplateDaoImpl templateDao = new TemplateDaoImpl();
        Long standardTemplateId = templateDao.findByCode("STANDARD").orElseThrow().getId();

        Page home = pageDao.findBySlug("home").orElseThrow();
        Page rolunk = pageDao.findBySlug("rolunk").orElseThrow();
        Page blockDemo = pageDao.findBySlug("block-demo").orElseThrow();

        assertDemoContentPage(home, "AI Coding Workflow Hub", HOME_CONTENT, standardTemplateId);
        assertDemoContentPage(rolunk, "About the AI Agent Workflow", ROLUNK_CONTENT, standardTemplateId);
        Assertions.assertEquals("AI Agent Block Demo", blockDemo.getTitle());
        Assertions.assertEquals(PageStatus.PUBLISHED, blockDemo.getStatus());
        Assertions.assertEquals(PageType.BLOCK, blockDemo.getPageType());
        Assertions.assertNull(blockDemo.getContent());
        Assertions.assertEquals(standardTemplateId, blockDemo.getTemplateId());
        Assertions.assertFalse(blockDemo.getHomepage());
        Assertions.assertTrue(blockDemo.getMenuVisible());

        assertDemoBlocks(pageBlockDao.findByPageId(blockDemo.getId()));

        DatabaseMigrationRunner.runMigrations();

        Assertions.assertEquals(1, countPages(pageDao, "home"));
        Assertions.assertEquals(1, countPages(pageDao, "rolunk"));
        Assertions.assertEquals(1, countPages(pageDao, "block-demo"));
        assertDemoBlocks(pageBlockDao.findByPageId(blockDemo.getId()));
    }

    @Test
    void aiCodingAgentMigrationPreservesCustomV13PagesAndBlockDemoConfiguration() throws Exception {
        try (Connection connection = DatabaseConfig.getConnection()) {
            try {
                createV15MigrationTestTables(connection);
                insertV15PreservationFixtures(connection);
                executeV15Migration(connection);

                assertPageValues(connection, "home", "Custom home title",
                        "<p>Welcome to the public test home page.</p>", "PUBLISHED", 1L, "CONTENT", "F", "T");
                assertPageValues(connection, "rolunk", "Custom rolunk title",
                        "<p>Rólunk: public test content.</p>", "PUBLISHED", 1L, "CONTENT", "F", "T");
                assertPageValues(connection, "block-demo", "AI Agent Block Demo", null, "DRAFT", 2L, "BLOCK", "T", "F");
                Assertions.assertEquals(1, countBlocks(connection));
            } finally {
                dropV15MigrationTestTables(connection);
            }
        }
    }

    @Test
    void enrichedBlockDemoMigrationRejectsLegacyAndDesiredCollisionWithoutMutation() throws Exception {
        try (Connection connection = DatabaseConfig.getConnection()) {
            try {
                createV15MigrationTestTables(connection);
                try (PreparedStatement templateStatement = connection.prepareStatement("""
                        INSERT INTO templates (id, code, active) VALUES (1, 'STANDARD', 'T')
                        """)) {
                    templateStatement.executeUpdate();
                }
                try (PreparedStatement pageStatement = connection.prepareStatement("""
                        INSERT INTO pages (id, title, slug, content, status, template_id, page_type, homepage, menu_visible)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """)) {
                    insertPage(pageStatement, 20L, "AI Agent Block Demo", "block-demo", null,
                            "PUBLISHED", 1L, "BLOCK", "F", "T");
                }
                try (PreparedStatement blockStatement = connection.prepareStatement("""
                        INSERT INTO page_blocks (page_id, block_type, title, sort_order, visible, config_json)
                        VALUES (?, 'HERO', 'Structured AI Coding Agents', 1, 'T', ?)
                        """)) {
                    blockStatement.setLong(1, 20L);
                    blockStatement.setString(2, HERO_CONFIG);
                    blockStatement.executeUpdate();
                    blockStatement.setLong(1, 20L);
                    blockStatement.setString(2, ENRICHED_HERO_CONFIG);
                    blockStatement.executeUpdate();
                }

                Assertions.assertThrows(Exception.class,
                        () -> executeMigration(connection, "db/migration/V16__enrich_ai_agent_block_demo.sql"));
                Assertions.assertEquals(2, countBlocks(connection, 20L));
                Assertions.assertEquals(1, countBlocksByConfig(connection, 20L, HERO_CONFIG));
                Assertions.assertEquals(1, countBlocksByConfig(connection, 20L, ENRICHED_HERO_CONFIG));
            } finally {
                dropV15MigrationTestTables(connection);
            }
        }
    }

    private static void createV15MigrationTestTables(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TEMPORARY TABLE templates (id BIGINT, code VARCHAR(255), active VARCHAR(1))");
            statement.execute("CREATE TEMPORARY TABLE pages (id BIGINT, title TEXT, slug VARCHAR(255), content TEXT, "
                    + "status VARCHAR(32), template_id BIGINT, page_type VARCHAR(32), homepage VARCHAR(1), "
                    + "menu_visible VARCHAR(1), created_at TIMESTAMP, updated_at TIMESTAMP)");
            statement.execute("CREATE TEMPORARY TABLE page_blocks (page_id BIGINT, block_type VARCHAR(255), "
                    + "title TEXT, sort_order INTEGER, visible VARCHAR(1), config_json TEXT, created_at TIMESTAMP, "
                    + "updated_at TIMESTAMP)");
        }
    }

    private static void dropV15MigrationTestTables(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS pg_temp.page_blocks, pg_temp.pages, pg_temp.templates");
        }
    }

    private static void insertV15PreservationFixtures(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO templates (id, code, active) VALUES (1, 'STANDARD', 'T'), (2, 'CUSTOM', 'T')
                """)) {
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO pages (id, title, slug, content, status, template_id, page_type, homepage, menu_visible)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            insertPage(statement, 10L, "Custom home title", "home", "<p>Welcome to the public test home page.</p>",
                    "PUBLISHED", 1L, "CONTENT", "F", "T");
            insertPage(statement, 11L, "Custom rolunk title", "rolunk", "<p>Rólunk: public test content.</p>",
                    "PUBLISHED", 1L, "CONTENT", "F", "T");
            insertPage(statement, 12L, "AI Agent Block Demo", "block-demo", null, "DRAFT", 2L, "BLOCK", "T", "F");
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO page_blocks (page_id, block_type, title, sort_order, visible, config_json)
                VALUES (12, 'CUSTOM', 'Custom block', 99, 'T', '{}')
                """)) {
            statement.executeUpdate();
        }
    }

    private static void insertPage(PreparedStatement statement, Long id, String title, String slug, String content,
                                   String status, Long templateId, String pageType, String homepage,
                                   String menuVisible) throws Exception {
        statement.setLong(1, id);
        statement.setString(2, title);
        statement.setString(3, slug);
        statement.setString(4, content);
        statement.setString(5, status);
        statement.setLong(6, templateId);
        statement.setString(7, pageType);
        statement.setString(8, homepage);
        statement.setString(9, menuVisible);
        statement.executeUpdate();
    }

    private static void executeV15Migration(Connection connection) throws Exception {
        executeMigration(connection, "db/migration/V15__ai_coding_agent_demo_content.sql");
    }

    private static void executeMigration(Connection connection, String resourcePath) throws Exception {
        try (InputStream input = SpecializedDaoReadIntegrationTest.class.getClassLoader()
                .getResourceAsStream(resourcePath)) {
            Assertions.assertNotNull(input);
            String migration = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            for (String sql : migration.split(";")) {
                if (!sql.isBlank()) {
                    try (Statement statement = connection.createStatement()) {
                        statement.execute(sql);
                    }
                }
            }
        }
    }

    private static void assertPageValues(Connection connection, String slug, String title, String content,
                                         String status, Long templateId, String pageType, String homepage,
                                         String menuVisible)
            throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT title, content, status, template_id, page_type, homepage, menu_visible
                FROM pages WHERE slug = ?
                """)) {
            statement.setString(1, slug);
            try (ResultSet resultSet = statement.executeQuery()) {
                Assertions.assertTrue(resultSet.next());
                Assertions.assertEquals(title, resultSet.getString("title"));
                Assertions.assertEquals(content, resultSet.getString("content"));
                Assertions.assertEquals(status, resultSet.getString("status"));
                Assertions.assertEquals(templateId, resultSet.getLong("template_id"));
                Assertions.assertEquals(pageType, resultSet.getString("page_type"));
                Assertions.assertEquals(homepage, resultSet.getString("homepage"));
                Assertions.assertEquals(menuVisible, resultSet.getString("menu_visible"));
                Assertions.assertFalse(resultSet.next());
            }
        }
    }

    private static int countBlocks(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*) FROM page_blocks WHERE page_id = 12
                """)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private static int countBlocks(Connection connection, Long pageId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*) FROM page_blocks WHERE page_id = ?
                """)) {
            statement.setLong(1, pageId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private static int countBlocksByConfig(Connection connection, Long pageId, String configJson) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*) FROM page_blocks WHERE page_id = ? AND config_json = ?
                """)) {
            statement.setLong(1, pageId);
            statement.setString(2, configJson);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private static void assertDemoContentPage(Page page, String title, String content, Long standardTemplateId) {
        Assertions.assertEquals(title, page.getTitle());
        Assertions.assertEquals(content, page.getContent());
        Assertions.assertEquals(PageStatus.PUBLISHED, page.getStatus());
        Assertions.assertEquals(PageType.CONTENT, page.getPageType());
        Assertions.assertEquals(standardTemplateId, page.getTemplateId());
        Assertions.assertFalse(page.getHomepage());
        Assertions.assertTrue(page.getMenuVisible());
    }

    private static void assertDemoBlocks(List<PageBlock> blocks) {
        Assertions.assertEquals(5, blocks.size());
        assertDemoBlockContains(blocks.get(0), "HERO", "Structured AI Coding Agents", 1,
                "Use specialized agents for planning, implementation, review and iterative fixing");
        assertDemoBlockContains(blocks.get(1), "TEXT", "A workflow instead of random code generation", 2,
                "AI-assisted development becomes much more useful when agents follow a clear software delivery process.");
        assertDemoBlockContains(blocks.get(2), "TEXT", "Separate agents, clear contracts", 3,
                "The connection between these agents is the integration contract.");
        assertDemoBlockContains(blocks.get(3), "TEXT", "Review-driven iteration", 4,
                "The reviewer role is important because generated code still needs engineering judgment.");
        assertDemoBlockContains(blocks.get(4), "CTA", "Continue with the workflow overview", 5,
                "Read more about how Explorer, Planner, Implementer, Reviewer and Fixer agents can work together");
    }

    private static void assertDemoBlockContains(PageBlock block, String type, String title, int sortOrder,
                                                String expectedConfigFragment) {
        Assertions.assertEquals(type, block.getBlockType());
        Assertions.assertEquals(title, block.getTitle());
        Assertions.assertEquals(sortOrder, block.getSortOrder());
        Assertions.assertTrue(block.isVisible());
        Assertions.assertTrue(block.getConfigJson().contains(expectedConfigFragment));
        Assertions.assertDoesNotThrow(() -> JsonParser.parseString(block.getConfigJson()));
    }

    private static void assertDemoBlock(PageBlock block, String type, String title, int sortOrder, String configJson) {
        Assertions.assertEquals(type, block.getBlockType());
        Assertions.assertEquals(title, block.getTitle());
        Assertions.assertEquals(sortOrder, block.getSortOrder());
        Assertions.assertTrue(block.isVisible());
        Assertions.assertEquals(configJson, block.getConfigJson());
        Assertions.assertDoesNotThrow(() -> JsonParser.parseString(block.getConfigJson()));
    }

    private static int countPages(PageDaoImpl pageDao, String slug) {
        return pageDao.findBySlug(slug).isPresent() ? 1 : 0;
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
