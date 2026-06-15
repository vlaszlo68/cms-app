package hu.laci.cms.backend.config.media;

import hu.laci.cms.backend.dao.media.MediaContentDaoImpl;
import hu.laci.cms.backend.model.media.MediaStorageType;
import hu.laci.cms.backend.service.media.DatabaseMediaStorageService;
import hu.laci.cms.backend.service.media.FileSystemMediaStorageService;
import hu.laci.cms.backend.service.media.MediaStorageService;
import hu.laci.cms.backend.service.media.MinioMediaStorageService;
import hu.laci.cms.backend.service.media.S3MediaStorageService;

public final class MediaStorageServiceFactory {

    private MediaStorageServiceFactory() {
    }

    public static MediaStorageService create(MediaStorageConfig config) {
        MediaStorageType storageType = config.getStorageType();
        return switch (storageType) {
            case FILESYSTEM -> new FileSystemMediaStorageService(config.getFilesystemPath());
            case DATABASE -> new DatabaseMediaStorageService(new MediaContentDaoImpl());
            case MINIO -> new MinioMediaStorageService();
            case S3 -> new S3MediaStorageService();
        };
    }
}
