package hu.laci.cms.backend.service.media;

import hu.laci.cms.backend.dao.media.MediaDao;
import hu.laci.cms.backend.dto.media.MediaResponse;
import hu.laci.cms.backend.dto.media.UploadMediaResponse;
import hu.laci.cms.backend.model.common.QuerySpec;
import hu.laci.cms.backend.model.media.Media;
import hu.laci.cms.backend.model.media.MediaProperty;
import hu.laci.cms.backend.model.media.MediaStorageType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

class MediaServiceTest {

    @Test
    void getMediaReturnsMetadataWithoutLoadingContent() {
        FakeMediaStorageService storageService = new FakeMediaStorageService("hello".getBytes());
        MediaService mediaService = new MediaService(new FakeMediaDao(testMedia()), storageService,
                MediaStorageType.DATABASE);

        MediaResponse response = mediaService.getMedia(42L);

        Assertions.assertEquals(42L, response.getId());
        Assertions.assertEquals("sample.txt", response.getOriginalFileName());
        Assertions.assertEquals("text/plain", response.getMimeType());
        Assertions.assertEquals(0, storageService.loadCalls);
    }

    @Test
    void getMediaContentLoadsStoredContent() {
        byte[] content = "hello".getBytes();
        FakeMediaStorageService storageService = new FakeMediaStorageService(content);
        MediaService mediaService = new MediaService(new FakeMediaDao(testMedia()), storageService,
                MediaStorageType.DATABASE);

        MediaContent response = mediaService.getMediaContent(42L);

        Assertions.assertEquals("sample.txt", response.getOriginalFileName());
        Assertions.assertEquals("text/plain", response.getMimeType());
        Assertions.assertEquals(content.length, response.getFileSize());
        Assertions.assertArrayEquals(content, response.getContent());
        Assertions.assertEquals(1, storageService.loadCalls);
    }

    @Test
    void uploadStoresCompletesAndPersistsMetadata() {
        MediaDao mediaDao = Mockito.mock(MediaDao.class);
        MediaStorageService storage = Mockito.mock(MediaStorageService.class);
        StoredMediaFile temporary = new StoredMediaFile("temporary", "temp:1", 3L);
        StoredMediaFile completed = new StoredMediaFile("33", "database:33", 3L);
        Mockito.when(storage.store(Mockito.eq("photo.png"), Mockito.any(), Mockito.eq("image/png")))
                .thenReturn(temporary);
        Mockito.when(mediaDao.create(Mockito.any())).thenAnswer(invocation -> {
            Media media = invocation.getArgument(0, Media.class);
            media.setId(33L);
            return media;
        });
        Mockito.when(storage.completeStore(33L, temporary)).thenReturn(completed);
        Mockito.when(mediaDao.update(Mockito.any())).thenAnswer(invocation -> invocation.getArgument(0, Media.class));
        MediaService service = new MediaService(mediaDao, storage, MediaStorageType.DATABASE);

        UploadMediaResponse response = service.uploadMedia("photo.png", new ByteArrayInputStream(new byte[]{1, 2, 3}),
                "image/png", " Cover ");

        Assertions.assertEquals(33L, response.getId());
        Assertions.assertEquals("Cover", response.getDescription());
        Mockito.verify(storage).completeStore(33L, temporary);
        Mockito.verify(mediaDao).update(Mockito.any());
    }

    @Test
    void failedMetadataCreateCleansUpStoredFile() {
        MediaDao mediaDao = Mockito.mock(MediaDao.class);
        MediaStorageService storage = Mockito.mock(MediaStorageService.class);
        StoredMediaFile temporary = new StoredMediaFile("temporary", "temp:1", 3L);
        Mockito.when(storage.store(Mockito.anyString(), Mockito.any(), Mockito.anyString())).thenReturn(temporary);
        Mockito.when(mediaDao.create(Mockito.any())).thenThrow(new IllegalStateException("database unavailable"));
        MediaService service = new MediaService(mediaDao, storage, MediaStorageType.DATABASE);

        MediaServiceException exception = Assertions.assertThrows(MediaServiceException.class,
                () -> service.uploadMedia("photo.png", new ByteArrayInputStream(new byte[]{1}), "image/png", null));

        Assertions.assertEquals(MediaService.STORAGE_ERROR, exception.getCode());
        Mockito.verify(storage).delete("temp:1");
    }

    @Test
    void deleteRemovesStorageThenMetadata() {
        MediaDao mediaDao = Mockito.mock(MediaDao.class);
        MediaStorageService storage = Mockito.mock(MediaStorageService.class);
        Media media = testMedia();
        Mockito.when(mediaDao.findById(42L)).thenReturn(Optional.of(media));
        Mockito.when(mediaDao.delete(media)).thenReturn(true);
        MediaService service = new MediaService(mediaDao, storage, MediaStorageType.DATABASE);

        Assertions.assertTrue(service.deleteMedia(42L));

        Mockito.verify(storage).delete("database:42");
        Mockito.verify(mediaDao).delete(media);
    }

    @Test
    void deleteStorageFailureDoesNotDeleteMetadata() {
        MediaDao mediaDao = Mockito.mock(MediaDao.class);
        MediaStorageService storage = Mockito.mock(MediaStorageService.class);
        Media media = testMedia();
        Mockito.when(mediaDao.findById(42L)).thenReturn(Optional.of(media));
        Mockito.doThrow(new IllegalStateException("storage unavailable")).when(storage).delete("database:42");
        MediaService service = new MediaService(mediaDao, storage, MediaStorageType.DATABASE);

        MediaServiceException exception = Assertions.assertThrows(MediaServiceException.class,
                () -> service.deleteMedia(42L));

        Assertions.assertEquals(MediaService.STORAGE_ERROR, exception.getCode());
        Mockito.verify(mediaDao, Mockito.never()).delete(Mockito.any());
    }

    @Test
    void uploadRejectsMissingRequiredFileInformation() {
        MediaService service = new MediaService(Mockito.mock(MediaDao.class), Mockito.mock(MediaStorageService.class),
                MediaStorageType.DATABASE);

        MediaServiceException exception = Assertions.assertThrows(MediaServiceException.class,
                () -> service.uploadMedia(" ", null, "", null));

        Assertions.assertEquals(MediaService.VALIDATION_ERROR, exception.getCode());
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
            Assertions.assertEquals("database:42", storagePath);
            return content;
        }
    }
}
