package hu.laci.cms.backend.dto.media;

import hu.laci.cms.backend.model.media.MediaStorageType;

public class MediaResponse {

    private final Long id;
    private final String originalFileName;
    private final String mimeType;
    private final Long fileSize;
    private final String description;
    private final MediaStorageType storageType;
    private final Boolean active;
    private final String createdAt;
    private final String updatedAt;

    public MediaResponse(Long id, String originalFileName, String mimeType, Long fileSize, String description,
                         MediaStorageType storageType, Boolean active, String createdAt, String updatedAt) {
        this.id = id;
        this.originalFileName = originalFileName;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.description = description;
        this.storageType = storageType;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public String getDescription() {
        return description;
    }

    public MediaStorageType getStorageType() {
        return storageType;
    }

    public Boolean getActive() {
        return active;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
}
