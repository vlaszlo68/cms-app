package hu.laci.cms.backend.model.media;

import hu.laci.cms.backend.model.common.AuditableProperty;
import hu.laci.cms.backend.model.common.BaseProperty;

public final class MediaProperty extends AuditableProperty {

    public static final MediaProperty ID = new MediaProperty(BaseProperty.ID.getPropertyName());
    public static final MediaProperty ORIGINAL_FILE_NAME = new MediaProperty("originalFileName");
    public static final MediaProperty STORED_FILE_NAME = new MediaProperty("storedFileName");
    public static final MediaProperty MIME_TYPE = new MediaProperty("mimeType");
    public static final MediaProperty FILE_SIZE = new MediaProperty("fileSize");
    public static final MediaProperty STORAGE_PATH = new MediaProperty("storagePath");
    public static final MediaProperty DESCRIPTION = new MediaProperty("description");
    public static final MediaProperty STORAGE_TYPE = new MediaProperty("storageType");
    public static final MediaProperty ACTIVE = new MediaProperty("active");
    public static final MediaProperty CREATED_AT = new MediaProperty(AuditableProperty.CREATED_AT.getPropertyName());
    public static final MediaProperty UPDATED_AT = new MediaProperty(AuditableProperty.UPDATED_AT.getPropertyName());
    public static final MediaProperty CREATED_BY = new MediaProperty(AuditableProperty.CREATED_BY.getPropertyName());
    public static final MediaProperty UPDATED_BY = new MediaProperty(AuditableProperty.UPDATED_BY.getPropertyName());

    private MediaProperty(String propertyName) {
        super(Media.class, propertyName);
    }
}
