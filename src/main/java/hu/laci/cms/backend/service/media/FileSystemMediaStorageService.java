package hu.laci.cms.backend.service.media;

import hu.laci.cms.backend.model.media.MediaStorageType;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

public class FileSystemMediaStorageService implements MediaStorageService {

    private final Path storageDirectory;

    public FileSystemMediaStorageService(Path storageDirectory) {
        this.storageDirectory = storageDirectory.toAbsolutePath().normalize();
    }

    @Override
    public StoredMediaFile store(String originalFileName, InputStream inputStream, String mimeType) {
        if (inputStream == null) {
            throw new MediaStorageException("Input stream is required.");
        }

        try {
            Files.createDirectories(storageDirectory);
            String storedFileName = UUID.randomUUID() + extractExtension(originalFileName);
            Path targetPath = storageDirectory.resolve(storedFileName).normalize();
            if (!targetPath.startsWith(storageDirectory)) {
                throw new MediaStorageException("Invalid target storage path.");
            }

            long fileSize = Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            return new StoredMediaFile(storedFileName, targetPath.toString(), fileSize);
        } catch (IOException e) {
            throw new MediaStorageException("Failed to store media file on filesystem.", e);
        }
    }

    @Override
    public void delete(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return;
        }

        try {
            Path targetPath = Path.of(storagePath).toAbsolutePath().normalize();
            if (!targetPath.startsWith(storageDirectory)) {
                throw new MediaStorageException("Refusing to delete file outside media storage directory.");
            }
            Files.deleteIfExists(targetPath);
        } catch (IOException e) {
            throw new MediaStorageException("Failed to delete media file from filesystem.", e);
        }
    }

    @Override
    public byte[] load(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            throw new MediaStorageException("Media storage path is required.");
        }

        try {
            Path targetPath = Path.of(storagePath).toAbsolutePath().normalize();
            if (!targetPath.startsWith(storageDirectory)) {
                throw new MediaStorageException("Refusing to read file outside media storage directory.");
            }
            return Files.readAllBytes(targetPath);
        } catch (IOException e) {
            throw new MediaStorageException("Failed to read media file from filesystem.", e);
        }
    }

    private static String extractExtension(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return "";
        }

        String sanitizedName = Path.of(originalFileName).getFileName().toString();
        int dotIndex = sanitizedName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == sanitizedName.length() - 1) {
            return "";
        }

        return sanitizedName.substring(dotIndex).toLowerCase(Locale.ROOT);
    }

    public MediaStorageType getStorageType() {
        return MediaStorageType.FILESYSTEM;
    }
}
