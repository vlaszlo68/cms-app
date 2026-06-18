package hu.laci.cms.backend.service.template;

import hu.laci.cms.backend.config.database.DatabaseConfig;
import hu.laci.cms.backend.config.database.migration.DatabaseMigrationRunner;
import hu.laci.cms.backend.config.session.SessionContext;
import hu.laci.cms.backend.dao.common.DaoRegistry;
import hu.laci.cms.backend.dao.settings.SiteSettingsDao;
import hu.laci.cms.backend.dao.template.TemplateDao;
import hu.laci.cms.backend.dto.settings.SaveSiteSettingsRequest;
import hu.laci.cms.backend.dto.settings.SiteSettingsResponse;
import hu.laci.cms.backend.dto.template.CreateTemplateRequest;
import hu.laci.cms.backend.dto.template.TemplateResponse;
import hu.laci.cms.backend.dto.template.UpdateTemplateRequest;
import hu.laci.cms.backend.model.settings.SiteSettings;
import hu.laci.cms.backend.model.template.Template;
import hu.laci.cms.backend.service.settings.SiteSettingsService;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemplateAndSiteSettingsServiceTest {

    private static final String PREFIX = "template_service_test_";

    private TemplateService templateService;
    private SiteSettingsService siteSettingsService;

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
        TemplateDao templateDao = DaoRegistry.getDao(Template.class);
        SiteSettingsDao siteSettingsDao = DaoRegistry.getDao(SiteSettings.class);
        templateService = new TemplateService(templateDao);
        siteSettingsService = new SiteSettingsService(siteSettingsDao);
        deleteTestTemplates();
    }

    @AfterEach
    void tearDown() throws SQLException {
        SessionContext.clear();
        deleteTestTemplates();
        siteSettingsService.saveSettings(new SaveSiteSettingsRequest(null, null, null, null,
                null, null, null));
    }

    @Test
    void migrationCreatesDefaultTemplates() {
        assertTrue(templateService.findByCode("STANDARD").isActive());
        assertTrue(templateService.findByCode("LANDING").isActive());
        assertTrue(templateService.findByCode("BLOG").isActive());
    }

    @Test
    void templateCrudAndDeactivateWork() {
        TemplateResponse created = templateService.createTemplate(new CreateTemplateRequest(
                PREFIX + "CODE", " Test Template ", " Description ", 42L, true));

        assertNotNull(created.getId());
        assertEquals("Test Template", created.getName());
        assertEquals("Description", created.getDescription());
        assertEquals(42L, created.getPreviewImageMediaId());

        TemplateResponse updated = templateService.updateTemplate(created.getId(), new UpdateTemplateRequest(
                PREFIX + "UPDATED", "Updated", null, null, true));
        assertEquals(PREFIX + "UPDATED", updated.getCode());

        TemplateResponse deactivated = templateService.deactivateTemplate(created.getId());
        assertFalse(deactivated.isActive());
    }

    @Test
    void duplicateTemplateCodeIsRejected() {
        templateService.createTemplate(new CreateTemplateRequest(PREFIX + "DUP", "First", null, null, true));

        TemplateServiceException exception = assertThrows(TemplateServiceException.class,
                () -> templateService.createTemplate(
                        new CreateTemplateRequest(PREFIX + "DUP", "Second", null, null, true)));

        assertEquals(TemplateService.DUPLICATE_CODE, exception.getCode());
    }

    @Test
    void siteSettingsSaveUpdatesSingletonRecord() throws SQLException {
        SiteSettingsResponse before = siteSettingsService.getSettings();
        SiteSettingsResponse saved = siteSettingsService.saveSettings(new SaveSiteSettingsRequest(
                " CMS Site ", 15L, " Footer ", " info@example.com ", " +36 1 234 ",
                " https://facebook.com/example ", " https://linkedin.com/company/example "));

        assertEquals(before.getId(), saved.getId());
        assertEquals("CMS Site", saved.getSiteName());
        assertEquals(15L, saved.getLogoMediaId());
        assertEquals("Footer", saved.getFooterText());
        assertEquals("info@example.com", saved.getContactEmail());
        assertEquals("+36 1 234", saved.getPhone());
        assertEquals(1, countSettingsRows());
    }

    @Test
    void findActiveExcludesDeactivatedTemplate() {
        TemplateResponse created = templateService.createTemplate(new CreateTemplateRequest(
                PREFIX + "ACTIVE", "Active", null, null, true));
        templateService.deactivateTemplate(created.getId());

        TemplateDao templateDao = DaoRegistry.getDao(Template.class);
        List<Long> activeIds = templateDao.findActive().stream().map(Template::getId).toList();

        assertFalse(activeIds.contains(created.getId()));
    }

    private static void deleteTestTemplates() throws SQLException {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM templates WHERE code LIKE ?")) {
            statement.setString(1, PREFIX + "%");
            statement.executeUpdate();
        }
    }

    private static int countSettingsRows() throws SQLException {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM site_settings");
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private static ServletContext createEmptyServletContext() {
        return (ServletContext) Proxy.newProxyInstance(
                TemplateAndSiteSettingsServiceTest.class.getClassLoader(),
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
