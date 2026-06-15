package hu.laci.cms.backend.model.media;

import hu.laci.cms.backend.dao.common.annotations.DbColumn;
import hu.laci.cms.backend.dao.common.annotations.DbTable;
import hu.laci.cms.backend.model.common.AuditableEntity;

@DbTable("media")
public class Media extends AuditableEntity {

    @DbColumn("original_file_name")
    private String originalFileName;

    @DbColumn("stored_file_name")
    private String storedFileName;

    @DbColumn("mime_type")
    private String mimeType;

    @DbColumn("file_size")
    private Long fileSize;

    @DbColumn("storage_path")
    private String storagePath;

    @DbColumn("description")
    private String description;

    @DbColumn("storage_type")
    private MediaStorageType storageType;

    @DbColumn("active")
    private Boolean active = Boolean.TRUE;

    public Media() {
    }

    public Media(Long id, String originalFileName, String storedFileName, String mimeType, Long fileSize,
                 String storagePath, String description, MediaStorageType storageType, Boolean active) {
        setId(id);
        this.originalFileName = originalFileName;
        this.storedFileName = storedFileName;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.storagePath = storagePath;
        this.description = description;
        this.storageType = storageType;
        this.active = active;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getStoredFileName() {
        return storedFileName;
    }

    public void setStoredFileName(String storedFileName) {
        this.storedFileName = storedFileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MediaStorageType getStorageType() {
        return storageType;
    }

    public void setStorageType(MediaStorageType storageType) {
        this.storageType = storageType;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
