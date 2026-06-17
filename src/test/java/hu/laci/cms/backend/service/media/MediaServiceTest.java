package hu.laci.cms.backend.service.media;

import hu.laci.cms.backend.dao.media.MediaDao;
import hu.laci.cms.backend.dto.media.MediaResponse;
import hu.laci.cms.backend.model.common.QuerySpec;
import hu.laci.cms.backend.model.media.Media;
import hu.laci.cms.backend.model.media.MediaProperty;
import hu.laci.cms.backend.model.media.MediaStorageType;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MediaServiceTest {

    @Test
    void getMediaReturnsMetadataWithoutLoadingContent() {
        FakeMediaStorageService storageService = new FakeMediaStorageService("hello".getBytes());
        MediaService mediaService = new MediaService(new FakeMediaDao(testMedia()), storageService,
                MediaStorageType.DATABASE);

        MediaResponse response = mediaService.getMedia(42L);

        assertEquals(42L, response.getId());
        assertEquals("sample.txt", response.getOriginalFileName());
        assertEquals("text/plain", response.getMimeType());
        assertEquals(0, storageService.loadCalls);
    }

    @Test
    void getMediaContentLoadsStoredContent() {
        byte[] content = "hello".getBytes();
        FakeMediaStorageService storageService = new FakeMediaStorageService(content);
        MediaService mediaService = new MediaService(new FakeMediaDao(testMedia()), storageService,
                MediaStorageType.DATABASE);

        MediaContent response = mediaService.getMediaContent(42L);

        assertEquals("sample.txt", response.getOriginalFileName());
        assertEquals("text/plain", response.getMimeType());
        assertEquals(content.length, response.getFileSize());
        assertArrayEquals(content, response.getContent());
        assertEquals(1, storageService.loadCalls);
    }

    private static Media testMedia() {
        Media media = new Media(42L, "sample.txt", "42", "text/plain", 5L,
                "database:42", "Sample", MediaStorageType.DATABASE, Boolean.TRUE);
        return media;
    }

    private static final class FakeMediaDao implements MediaDao {

        private final Media media;

        private FakeMediaDao(Media media) {
            this.media = media;
        }

        @Override
        public List<Media> findActive() {
            return List.of(media);
        }

        @Override
        public List<Media> findAll(QuerySpec<MediaProperty> querySpec) {
            return List.of(media);
        }

        @Override
        public Optional<Media> findById(Long id) {
            return media.getId().equals(id) ? Optional.of(media) : Optional.empty();
        }

        @Override
        public Media save(Media entity) {
            return entity;
        }

        @Override
        public Media create(Media entity) {
            return entity;
        }

        @Override
        public Media update(Media entity) {
            return entity;
        }

        @Override
        public boolean deleteById(Long id) {
            return true;
        }

        @Override
        public boolean delete(Media entity) {
            return true;
        }
    }

    private static final class FakeMediaStorageService implements MediaStorageService {

        private final byte[] content;
        private int loadCalls;

        private FakeMediaStorageService(byte[] content) {
            this.content = content;
        }

        @Override
        public StoredMediaFile store(String originalFileName, InputStream inputStream, String mimeType) {
            return new StoredMediaFile("stored", "database:42", content.length);
        }

        @Override
        public void delete(String storagePath) {
        }

        @Override
        public byte[] load(String storagePath) {
            loadCalls++;
            assertEquals("database:42", storagePath);
            return content;
        }
    }
}
