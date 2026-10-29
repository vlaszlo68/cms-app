package hu.laci.cms.backend.config.database.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Static contract checks for the data-preserving multi-Site migration. */
class MultiSiteMigrationContractTest {

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
}
