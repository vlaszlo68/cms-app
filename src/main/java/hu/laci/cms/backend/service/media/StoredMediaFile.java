package hu.laci.cms.backend.service.media;

public class StoredMediaFile {

    private String storedFileName;
    private String storagePath;
    private long fileSize;

    public StoredMediaFile() {
    }

    public StoredMediaFile(String storedFileName, String storagePath, long fileSize) {
        this.storedFileName = storedFileName;
        this.storagePath = storagePath;
        this.fileSize = fileSize;
    }

    public String getStoredFileName() {
        return storedFileName;
    }

    public void setStoredFileName(String storedFileName) {
        this.storedFileName = storedFileName;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
}
