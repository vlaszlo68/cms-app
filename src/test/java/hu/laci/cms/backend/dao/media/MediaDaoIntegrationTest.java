package hu.laci.cms.backend.dao.media;

import hu.laci.cms.backend.config.database.DatabaseConfig;
import hu.laci.cms.backend.config.database.migration.DatabaseMigrationRunner;
import hu.laci.cms.backend.config.session.SessionContext;
import hu.laci.cms.backend.model.media.Media;
import hu.laci.cms.backend.model.media.MediaStorageType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletContext;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/** Database-backed integration tests for media metadata and binary-content DAOs. */
class MediaDaoIntegrationTest {

    private static final String PREFIX = "media_dao_test_";
    private MediaDao mediaDao;
    private MediaContentDao contentDao;

    @BeforeAll
    static void initializeDatabase() {
        DatabaseConfig.initialize(emptyServletContext());
        DatabaseMigrationRunner.runMigrations();
    }

    @AfterAll
    static void shutdownDatabase() {
        DatabaseConfig.shutdown();
    }

    @BeforeEach
    void setUp() throws SQLException {
        SessionContext.clear();
        deleteTestMedia();
        mediaDao = new MediaDaoImpl();
        contentDao = new MediaContentDaoImpl();
    }

    @AfterEach
    void cleanUp() throws SQLException {
        SessionContext.clear();
        deleteTestMedia();
    }

    @Test
    void createsLoadsUpdatesAndFiltersMediaMetadata() {
        Media active = mediaDao.create(media("active", true));
        Media inactive = mediaDao.create(media("inactive", false));
        active.setDescription("updated description");
        mediaDao.update(active);

        Media loaded = mediaDao.findById(active.getId()).orElseThrow();
        List<Media> activeMedia = mediaDao.findActive();

        Assertions.assertEquals("updated description", loaded.getDescription());
        Assertions.assertEquals(MediaStorageType.DATABASE, loaded.getStorageType());
        Assertions.assertTrue(activeMedia.stream().anyMatch(media -> media.getId().equals(active.getId())));
        Assertions.assertFalse(activeMedia.stream().anyMatch(media -> media.getId().equals(inactive.getId())));
    }

    @Test
    void savesLoadsUpdatesAndDeletesBinaryContent() {
        Media media = mediaDao.create(media("content", true));
        contentDao.saveContent(media.getId(), new byte[]{1, 2});
        Assertions.assertArrayEquals(new byte[]{1, 2}, contentDao.loadContent(media.getId()));
        contentDao.saveContent(media.getId(), new byte[]{3});
        Assertions.assertArrayEquals(new byte[]{3}, contentDao.loadContent(media.getId()));
        contentDao.deleteContent(media.getId());
        Assertions.assertNull(contentDao.loadContent(media.getId()));
    }

    private Media media(String suffix, boolean active) {
        return new Media(null, PREFIX + suffix + ".txt", suffix, "text/plain", 3L,
                "database:" + suffix, "test media", MediaStorageType.DATABASE, active);
    }

    private void deleteTestMedia() throws SQLException {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM media WHERE original_file_name LIKE ?")) {
            statement.setString(1, PREFIX + "%");
            statement.executeUpdate();
        }
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
