package hu.laci.cms.backend.service.media;

import hu.laci.cms.backend.dao.media.MediaDao;
import hu.laci.cms.backend.dto.media.MediaResponse;
import hu.laci.cms.backend.dto.media.UploadMediaResponse;
import hu.laci.cms.backend.model.common.QuerySpec;
import hu.laci.cms.backend.model.media.Media;
import hu.laci.cms.backend.model.media.MediaProperty;
import hu.laci.cms.backend.model.media.MediaStorageType;
import hu.laci.cms.backend.model.site.Site;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

public class MediaService {

    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String MEDIA_NOT_FOUND = "MEDIA_NOT_FOUND";
    public static final String STORAGE_ERROR = "STORAGE_ERROR";

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaService.class);

    private final MediaDao mediaDao;
    private final MediaStorageService mediaStorageService;
    private final MediaStorageType storageType;

    public MediaService(MediaDao mediaDao, MediaStorageService mediaStorageService, MediaStorageType storageType) {
        this.mediaDao = Objects.requireNonNull(mediaDao, "mediaDao must not be null");
        this.mediaStorageService = Objects.requireNonNull(mediaStorageService, "mediaStorageService must not be null");
        this.storageType = Objects.requireNonNull(storageType, "storageType must not be null");
    }

    public List<MediaResponse> listMedia(Site site, boolean activeOnly) {
        requireSite(site);
        List<Media> mediaItems = activeOnly
                ? mediaDao.findActiveBySite(site)
                : mediaDao.findAllBySite(site);
        return mediaItems.stream().map(this::toResponse).toList();
    }

    public MediaResponse getMedia(Site site, Long id) {
        return toResponse(loadMedia(site, id));
    }

    public MediaContent getMediaContent(Site site, Long id) {
        Media media = loadMedia(site, id);
        try {
            byte[] content = mediaStorageService.load(media.getStoragePath());
            return new MediaContent(media.getOriginalFileName(), media.getMimeType(), content.length, content);
        } catch (RuntimeException e) {
            throw new MediaServiceException(STORAGE_ERROR, "Failed to load media file.", e);
        }
    }

    public UploadMediaResponse uploadMedia(Site site, String originalFileName, InputStream inputStream, String mimeType,
                                           String description) {
        requireSite(site);
        validateUpload(originalFileName, inputStream, mimeType);

        StoredMediaFile storedMediaFile = null;
        Media createdMedia = null;
        try {
            storedMediaFile = mediaStorageService.store(originalFileName, inputStream, mimeType);
            Media media = new Media(
                    null,
                    trim(originalFileName),
                    storedMediaFile.getStoredFileName(),
                    trim(mimeType),
                    storedMediaFile.getFileSize(),
                    storedMediaFile.getStoragePath(),
                    trimToNull(description),
                    storageType,
                    Boolean.TRUE
            );
            media.setSite(site);

            createdMedia = mediaDao.create(media);
            storedMediaFile = mediaStorageService.completeStore(createdMedia.getId(), storedMediaFile);
            createdMedia.setStoredFileName(storedMediaFile.getStoredFileName());
            createdMedia.setStoragePath(storedMediaFile.getStoragePath());
            createdMedia.setFileSize(storedMediaFile.getFileSize());
            createdMedia = mediaDao.update(createdMedia);

            LOGGER.info("Uploaded media id={}, file={}", createdMedia.getId(), createdMedia.getOriginalFileName());
            return toUploadResponse(createdMedia);
        } catch (RuntimeException e) {
            cleanupFailedUpload(storedMediaFile, createdMedia);
            throw new MediaServiceException(STORAGE_ERROR, "Failed to store media file.", e);
        }
    }

    public boolean deleteMedia(Site site, Long id) {
        Media media = loadMedia(site, id);
        try {
            mediaStorageService.delete(media.getStoragePath());
            boolean deleted = mediaDao.delete(media);
            LOGGER.info("Deleted media id={}, deleted={}", id, deleted);
            return deleted;
        } catch (RuntimeException e) {
            throw new MediaServiceException(STORAGE_ERROR, "Failed to delete media file.", e);
        }
    }

    private Media loadMedia(Site site, Long id) {
        requireSite(site);
        if (id == null) {
            throw new MediaServiceException(VALIDATION_ERROR, "Media id is required.");
        }

        return mediaDao.findBySiteAndId(site, id)
                .orElseThrow(() -> new MediaServiceException(MEDIA_NOT_FOUND, "Media not found."));
    }

    private void validateUpload(String originalFileName, InputStream inputStream, String mimeType) {
        if (isBlank(originalFileName)) {
            throw new MediaServiceException(VALIDATION_ERROR, "originalFileName is required.");
        }
        if (inputStream == null) {
            throw new MediaServiceException(VALIDATION_ERROR, "file is required.");
        }
        if (isBlank(mimeType)) {
            throw new MediaServiceException(VALIDATION_ERROR, "mimeType is required.");
        }
    }

    private void cleanupFailedUpload(StoredMediaFile storedMediaFile, Media createdMedia) {
        if (storedMediaFile != null) {
            try {
                mediaStorageService.delete(storedMediaFile.getStoragePath());
            } catch (RuntimeException cleanupException) {
                LOGGER.warn("Failed to cleanup stored media file after upload failure.", cleanupException);
            }
        }
        if (createdMedia != null && createdMedia.getId() != null) {
            try {
                mediaDao.delete(createdMedia);
            } catch (RuntimeException cleanupException) {
                LOGGER.warn("Failed to cleanup media metadata after upload failure.", cleanupException);
            }
        }
    }

    private MediaResponse toResponse(Media media) {
        return new MediaResponse(
                media.getId(),
                media.getOriginalFileName(),
                media.getMimeType(),
                media.getFileSize(),
                media.getDescription(),
                media.getStorageType(),
                media.getActive(),
                toIsoString(media.getCreatedAt()),
                toIsoString(media.getUpdatedAt())
        );
    }

    private UploadMediaResponse toUploadResponse(Media media) {
        return new UploadMediaResponse(
                media.getId(),
                media.getOriginalFileName(),
                media.getMimeType(),
                media.getFileSize(),
                media.getDescription(),
                media.getStorageType(),
                media.getActive(),
                toIsoString(media.getCreatedAt()),
                toIsoString(media.getUpdatedAt())
        );
    }

    private static String toIsoString(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().toString();
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String trimToNull(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static void requireSite(Site site) {
        if (site == null || site.getId() == null) {
            throw new MediaServiceException(VALIDATION_ERROR, "Site is required.");
        }
    }
}
