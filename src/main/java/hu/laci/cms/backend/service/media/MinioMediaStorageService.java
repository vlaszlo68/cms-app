package hu.laci.cms.backend.service.media;

import java.io.InputStream;

public class MinioMediaStorageService implements MediaStorageService {

    @Override
    public StoredMediaFile store(String originalFileName, InputStream inputStream, String mimeType) {
        throw new UnsupportedOperationException("MINIO media storage is not implemented yet.");
    }

    @Override
    public void delete(String storagePath) {
        throw new UnsupportedOperationException("MINIO media storage is not implemented yet.");
    }
}
