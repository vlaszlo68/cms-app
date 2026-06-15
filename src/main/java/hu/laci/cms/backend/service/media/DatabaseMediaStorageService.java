package hu.laci.cms.backend.service.media;

import hu.laci.cms.backend.dao.media.MediaContentDao;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DatabaseMediaStorageService implements MediaStorageService {

    private static final String STORAGE_PREFIX = "database:";
    private static final int BUFFER_SIZE = 8192;

    private final MediaContentDao mediaContentDao;
    private final Map<String, byte[]> pendingContentByToken = new ConcurrentHashMap<>();

    public DatabaseMediaStorageService(MediaContentDao mediaContentDao) {
        this.mediaContentDao = mediaContentDao;
    }

    @Override
    public StoredMediaFile store(String originalFileName, InputStream inputStream, String mimeType) {
        if (inputStream == null) {
            throw new MediaStorageException("Input stream is required.");
        }

        String token = UUID.randomUUID().toString();
        byte[] content = readAllBytes(inputStream);
        pendingContentByToken.put(token, content);
        return new StoredMediaFile(token, STORAGE_PREFIX + token, content.length);
    }

    @Override
    public StoredMediaFile completeStore(Long mediaId, StoredMediaFile storedMediaFile) {
        if (mediaId == null) {
            throw new MediaStorageException("Media id is required to store database-backed content.");
        }
        if (storedMediaFile == null || storedMediaFile.getStoragePath() == null) {
            throw new MediaStorageException("Stored media file is required.");
        }

        String token = storedMediaFile.getStoragePath().substring(STORAGE_PREFIX.length());
        byte[] content = pendingContentByToken.remove(token);
        if (content == null) {
            throw new MediaStorageException("Pending database media content is missing.");
        }

        mediaContentDao.saveContent(mediaId, content);
        storedMediaFile.setStoragePath(STORAGE_PREFIX + mediaId);
        storedMediaFile.setStoredFileName(String.valueOf(mediaId));
        return storedMediaFile;
    }

    @Override
    public void delete(String storagePath) {
        if (storagePath == null || storagePath.isBlank() || !storagePath.startsWith(STORAGE_PREFIX)) {
            return;
        }

        String storageId = storagePath.substring(STORAGE_PREFIX.length());
        try {
            mediaContentDao.deleteContent(Long.parseLong(storageId));
        } catch (NumberFormatException e) {
            pendingContentByToken.remove(storageId);
        }
    }

    private static byte[] readAllBytes(InputStream inputStream) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[BUFFER_SIZE];
            int readBytes;
            while ((readBytes = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, readBytes);
            }
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new MediaStorageException("Failed to read database media content.", e);
        }
    }
}
