package hu.laci.cms.backend.config.database.migration;

import hu.laci.cms.backend.config.database.DatabaseConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.mockito.Mockito;

import javax.servlet.ServletContext;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Static contract checks for the data-preserving multi-Site migration. */
class MultiSiteMigrationContractTest {

    @BeforeAll
    static void initializeDatabase() {
        DatabaseConfig.initialize(Mockito.mock(ServletContext.class));
    }

    @Test
    void migrationBackfillsOwnersAndUsesCompositeUniqueness() throws IOException {
        try (InputStream input = getClass().getResourceAsStream(
                "/db/migration/V17__multi_site_foundation.sql")) {
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(sql.contains("VALUES ('default', 'Default Site'"));
            assertTrue(sql.contains("UPDATE pages SET site_id"));
            assertTrue(sql.contains("UPDATE menus SET site_id"));
            assertTrue(sql.contains("UPDATE site_settings SET site_id"));
            assertTrue(sql.contains("UPDATE media SET site_id"));
            assertTrue(sql.contains("UNIQUE INDEX uk_pages_site_slug ON pages (site_id, slug)"));
            assertTrue(sql.contains("UNIQUE INDEX uk_menus_site_code ON menus (site_id, code)"));
            assertTrue(sql.contains("UNIQUE INDEX uk_site_settings_site ON site_settings (site_id)"));
        }
    }

    @Test
    void siteDeletionThemeMigrationRemovesLegacySiteConstraintsAndDefaultsTheme() throws IOException {
        try (InputStream input = getClass().getResourceAsStream(
                "/db/migration/V19__site_deletion_and_theme_code.sql")) {
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(sql.contains("DROP CONSTRAINT IF EXISTS fk_pages_site"));
            assertTrue(sql.contains("DROP CONSTRAINT IF EXISTS fk_menus_site"));
            assertTrue(sql.contains("DROP CONSTRAINT IF EXISTS fk_site_settings_site"));
            assertTrue(sql.contains("DROP CONSTRAINT IF EXISTS fk_media_site"));
            assertTrue(sql.contains("DROP CONSTRAINT IF EXISTS applications_site_id_fkey"));
            assertTrue(sql.contains("ADD COLUMN theme_code VARCHAR(50)"));
            assertTrue(sql.contains("UPDATE sites SET theme_code = 'STANDARD'"));
            assertTrue(sql.contains("SET DEFAULT 'STANDARD'"));
            assertTrue(sql.contains("SET NOT NULL"));
        }
    }

    @Test
    void siteApplicationMigrationPreservesIdentifiersAndRemovesApplicationForeignKey() throws IOException {
        try (InputStream input = getClass().getResourceAsStream(
                "/db/migration/V20__rename_applications_to_site_applications.sql")) {
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(sql.contains("DROP CONSTRAINT IF EXISTS fk_menu_items_application"));
            assertTrue(sql.contains("ALTER TABLE applications RENAME TO site_applications"));
            assertTrue(sql.contains("RENAME COLUMN code TO application_code"));
            assertTrue(sql.contains("RENAME COLUMN application_id TO site_application_id"));
            assertTrue(sql.contains("RENAME CONSTRAINT uk_applications_site_code"));
            assertTrue(sql.contains("RENAME TO idx_site_applications_site"));
            assertFalse(sql.contains(" ON DELETE "));
        }
    }

    @Test
    void siteApplicationMigrationRenamesPopulatedLegacyRowsWithoutChangingIdentifiers() throws Exception {
        try (Connection connection = DatabaseConfig.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TEMPORARY TABLE applications ("
                        + "id BIGINT PRIMARY KEY, site_id BIGINT NOT NULL, code VARCHAR(100) NOT NULL, "
                        + "name VARCHAR(255) NOT NULL, active VARCHAR(1) NOT NULL, created_at TIMESTAMP NOT NULL, "
                        + "updated_at TIMESTAMP NOT NULL, created_by BIGINT, updated_by BIGINT, "
                        + "CONSTRAINT uk_applications_site_code UNIQUE (site_id, code))");
                statement.execute("CREATE INDEX idx_applications_site ON applications (site_id)");
                statement.execute("CREATE TEMPORARY TABLE menu_items (id BIGINT PRIMARY KEY, application_id BIGINT, "
                        + "CONSTRAINT fk_menu_items_application FOREIGN KEY (application_id) REFERENCES applications(id))");
            }
            try (PreparedStatement application = connection.prepareStatement("""
                    INSERT INTO applications (id, site_id, code, name, active, created_at, updated_at, created_by, updated_by)
                    VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?)
                    """)) {
                application.setLong(1, 10L);
                application.setLong(2, 1L);
                application.setString(3, "TEXT_ANALYZER");
                application.setString(4, "Text Analyzer");
                application.setString(5, "T");
                application.setLong(6, 7L);
                application.setLong(7, 8L);
                application.executeUpdate();
            }
            try (PreparedStatement menuItem = connection.prepareStatement(
                    "INSERT INTO menu_items (id, application_id) VALUES (?, ?)")) {
                menuItem.setLong(1, 20L);
                menuItem.setLong(2, 10L);
                menuItem.executeUpdate();
            }

            executeMigration(connection, "db/migration/V20__rename_applications_to_site_applications.sql");

            try (PreparedStatement application = connection.prepareStatement("""
                    SELECT id, site_id, application_code, name, active, created_by, updated_by
                    FROM site_applications WHERE id = ?
                    """)) {
                application.setLong(1, 10L);
                try (ResultSet rows = application.executeQuery()) {
                    assertTrue(rows.next());
                    assertEquals(1L, rows.getLong("site_id"));
                    assertEquals("TEXT_ANALYZER", rows.getString("application_code"));
                    assertEquals("Text Analyzer", rows.getString("name"));
                    assertEquals("T", rows.getString("active"));
                    assertEquals(7L, rows.getLong("created_by"));
                    assertEquals(8L, rows.getLong("updated_by"));
                    assertFalse(rows.next());
                }
            }
            try (PreparedStatement menuItem = connection.prepareStatement(
                    "SELECT site_application_id FROM menu_items WHERE id = ?")) {
                menuItem.setLong(1, 20L);
                try (ResultSet rows = menuItem.executeQuery()) {
                    assertTrue(rows.next());
                    assertEquals(10L, rows.getLong("site_application_id"));
                }
            }
            try (PreparedStatement oldTable = connection.prepareStatement("SELECT to_regclass('pg_temp.applications')");
                 ResultSet rows = oldTable.executeQuery()) {
                assertTrue(rows.next());
                assertNull(rows.getString(1));
            }
        }
    }

    private static void executeMigration(Connection connection, String resourcePath) throws Exception {
        try (InputStream input = MultiSiteMigrationContractTest.class.getClassLoader().getResourceAsStream(resourcePath)) {
            assertTrue(input != null);
            String migration = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            for (String statementSql : migration.split(";")) {
                if (!statementSql.isBlank()) {
                    try (Statement statement = connection.createStatement()) {
                        statement.execute(statementSql);
                    }
                }
            }
        }
    }
}
