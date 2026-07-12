package hu.laci.cms.backend.service.media;

import hu.laci.cms.backend.dao.media.MediaContentDao;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Unit tests for database and filesystem media storage implementations. */
class MediaStorageServiceTest {

    @Test
    void databaseStorageKeepsPendingContentThenPersistsItByMediaId() {
        MediaContentDao dao = Mockito.mock(MediaContentDao.class);
        DatabaseMediaStorageService storage = new DatabaseMediaStorageService(dao);
        byte[] bytes = new byte[]{1, 2, 3};

        StoredMediaFile pending = storage.store("photo.png", new ByteArrayInputStream(bytes), "image/png");
        Assertions.assertArrayEquals(bytes, storage.load(pending.getStoragePath()));
        StoredMediaFile completed = storage.completeStore(44L, pending);

        Assertions.assertEquals("database:44", completed.getStoragePath());
        Assertions.assertEquals("44", completed.getStoredFileName());
        Mockito.verify(dao).saveContent(44L, bytes);
    }

    @Test
    void databaseStorageLoadsAndDeletesPersistedContent() {
        MediaContentDao dao = Mockito.mock(MediaContentDao.class);
        Mockito.when(dao.loadContent(44L)).thenReturn(new byte[]{4, 5});
        DatabaseMediaStorageService storage = new DatabaseMediaStorageService(dao);

        Assertions.assertArrayEquals(new byte[]{4, 5}, storage.load("database:44"));
        storage.delete("database:44");

        Mockito.verify(dao).deleteContent(44L);
    }

    @Test
    void filesystemStorageRoundTripsContentAndRefusesOutsidePath() throws Exception {
        Path directory = Files.createTempDirectory("cms-media-storage-test-");
        FileSystemMediaStorageService storage = new FileSystemMediaStorageService(directory);
        StoredMediaFile stored = storage.store("PHOTO.PNG", new ByteArrayInputStream(new byte[]{7, 8}), "image/png");

        Assertions.assertTrue(stored.getStoredFileName().endsWith(".png"));
        Assertions.assertArrayEquals(new byte[]{7, 8}, storage.load(stored.getStoragePath()));
        Assertions.assertThrows(MediaStorageException.class, () -> storage.load(Path.of("outside-file").toString()));
        storage.delete(stored.getStoragePath());
        Assertions.assertFalse(Files.exists(Path.of(stored.getStoragePath())));
        Files.deleteIfExists(directory);
    }

    @Test
    void databaseStorageRejectsInvalidAndMissingContentStates() {
        MediaContentDao dao = Mockito.mock(MediaContentDao.class);
        DatabaseMediaStorageService storage = new DatabaseMediaStorageService(dao);

        Assertions.assertThrows(MediaStorageException.class, () -> storage.store("file", null, "text/plain"));
        Assertions.assertThrows(MediaStorageException.class,
                () -> storage.completeStore(null, new StoredMediaFile("token", "database:token", 1L)));
        Assertions.assertThrows(MediaStorageException.class,
                () -> storage.completeStore(1L, new StoredMediaFile("token", "database:missing", 1L)));
        Assertions.assertThrows(MediaStorageException.class, () -> storage.load("not-a-database-path"));
        Mockito.when(dao.loadContent(55L)).thenReturn(null);
        Assertions.assertThrows(MediaStorageException.class, () -> storage.load("database:55"));
    }

    @Test
    void databaseStorageDeletesPendingContentWithoutCallingDao() {
        MediaContentDao dao = Mockito.mock(MediaContentDao.class);
        DatabaseMediaStorageService storage = new DatabaseMediaStorageService(dao);
        StoredMediaFile pending = storage.store("file.txt", new ByteArrayInputStream(new byte[]{1}), "text/plain");

        storage.delete(pending.getStoragePath());

        Assertions.assertThrows(MediaStorageException.class, () -> storage.load(pending.getStoragePath()));
        Mockito.verifyNoInteractions(dao);
    }

    @Test
    void filesystemStorageRejectsMissingAndOutsidePaths() throws Exception {
        Path directory = Files.createTempDirectory("cms-media-storage-errors-");
        FileSystemMediaStorageService storage = new FileSystemMediaStorageService(directory);
        Path outside = directory.getParent().resolve("outside-media-file");

        Assertions.assertThrows(MediaStorageException.class, () -> storage.store("file", null, "text/plain"));
        Assertions.assertThrows(MediaStorageException.class, () -> storage.load(directory.resolve("missing").toString()));
        Assertions.assertThrows(MediaStorageException.class, () -> storage.delete(outside.toString()));
        Files.deleteIfExists(directory);
    }

    @Test
    void minioAndS3StorageFailExplicitlyUntilImplemented() {
        assertUnsupported(new MinioMediaStorageService());
        assertUnsupported(new S3MediaStorageService());
    }

    private void assertUnsupported(MediaStorageService storage) {
        Assertions.assertThrows(UnsupportedOperationException.class,
                () -> storage.store("file", new ByteArrayInputStream(new byte[]{1}), "text/plain"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> storage.load("path"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> storage.delete("path"));
    }
}
