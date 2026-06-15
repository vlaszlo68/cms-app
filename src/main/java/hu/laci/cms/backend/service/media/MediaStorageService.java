package hu.laci.cms.backend.service.media;

import java.io.InputStream;

public interface MediaStorageService {

    StoredMediaFile store(String originalFileName, InputStream inputStream, String mimeType);

    default StoredMediaFile completeStore(Long mediaId, StoredMediaFile storedMediaFile) {
        return storedMediaFile;
    }

    void delete(String storagePath);
}
