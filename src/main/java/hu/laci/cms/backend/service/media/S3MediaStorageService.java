package hu.laci.cms.backend.service.media;

import java.io.InputStream;

public class S3MediaStorageService implements MediaStorageService {

    @Override
    public StoredMediaFile store(String originalFileName, InputStream inputStream, String mimeType) {
        throw new UnsupportedOperationException("S3 media storage is not implemented yet.");
    }

    @Override
    public void delete(String storagePath) {
        throw new UnsupportedOperationException("S3 media storage is not implemented yet.");
    }
}
