package hu.laci.cms.backend.service.media;

public class MediaContent {

    private final String originalFileName;
    private final String mimeType;
    private final long fileSize;
    private final byte[] content;

    public MediaContent(String originalFileName, String mimeType, long fileSize, byte[] content) {
        this.originalFileName = originalFileName;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.content = content;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public byte[] getContent() {
        return content;
    }
}
