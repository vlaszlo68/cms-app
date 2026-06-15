package hu.laci.cms.backend.dto.media;

import hu.laci.cms.backend.model.media.MediaStorageType;

public class UploadMediaResponse extends MediaResponse {

    public UploadMediaResponse(Long id, String originalFileName, String mimeType, Long fileSize, String description,
                               MediaStorageType storageType, Boolean active, String createdAt, String updatedAt) {
        super(id, originalFileName, mimeType, fileSize, description, storageType, active, createdAt, updatedAt);
    }
}
