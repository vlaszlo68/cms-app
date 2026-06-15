CREATE TABLE IF NOT EXISTS media (
    id BIGSERIAL PRIMARY KEY,
    original_file_name VARCHAR(255) NOT NULL,
    stored_file_name VARCHAR(255),
    mime_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_path VARCHAR(1000),
    description VARCHAR(500),
    storage_type VARCHAR(30) NOT NULL,
    active VARCHAR(1) NOT NULL DEFAULT 'T',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by BIGINT,
    updated_by BIGINT,
    CONSTRAINT chk_media_original_file_name_not_blank CHECK (btrim(original_file_name) <> ''),
    CONSTRAINT chk_media_mime_type_not_blank CHECK (btrim(mime_type) <> ''),
    CONSTRAINT chk_media_file_size_non_negative CHECK (file_size >= 0),
    CONSTRAINT chk_media_storage_type CHECK (storage_type IN ('FILESYSTEM', 'DATABASE', 'MINIO', 'S3')),
    CONSTRAINT chk_media_active_boolean CHECK (active IN ('T', 'F'))
);

CREATE TABLE IF NOT EXISTS media_contents (
    id BIGSERIAL PRIMARY KEY,
    media_id BIGINT NOT NULL UNIQUE,
    content BYTEA NOT NULL,
    CONSTRAINT fk_media_contents_media
        FOREIGN KEY (media_id)
        REFERENCES media(id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_media_active ON media (active);

CREATE INDEX IF NOT EXISTS idx_media_storage_type ON media (storage_type);
