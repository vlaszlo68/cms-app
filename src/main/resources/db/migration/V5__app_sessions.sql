CREATE TABLE app_sessions (
    id_hash VARCHAR(128) PRIMARY KEY,
    user_id BIGINT NULL,
    login_name VARCHAR(255) NULL,
    email VARCHAR(255) NULL,
    role VARCHAR(50) NULL,
    csrf_token VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL,
    last_accessed_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    invalidated CHAR(1) NOT NULL DEFAULT 'F'
);

CREATE TABLE app_session_attributes (
    id BIGSERIAL PRIMARY KEY,
    session_id_hash VARCHAR(128) NOT NULL,
    attribute_name VARCHAR(100) NOT NULL,
    attribute_type VARCHAR(100) NOT NULL,
    json_value TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_app_session_attributes_session
        FOREIGN KEY (session_id_hash)
        REFERENCES app_sessions (id_hash)
        ON DELETE CASCADE,

    CONSTRAINT uq_app_session_attributes_name
        UNIQUE (session_id_hash, attribute_name)
);

CREATE INDEX idx_app_sessions_expires_at ON app_sessions (expires_at);
CREATE INDEX idx_app_session_attributes_session ON app_session_attributes (session_id_hash);
CREATE INDEX idx_app_sessions_user_id ON app_sessions (user_id);
