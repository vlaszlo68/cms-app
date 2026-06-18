CREATE TABLE templates (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    preview_image_media_id BIGINT,
    active VARCHAR(1) NOT NULL DEFAULT 'T',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by BIGINT,
    updated_by BIGINT,
    CONSTRAINT chk_templates_code_not_blank CHECK (btrim(code) <> ''),
    CONSTRAINT chk_templates_name_not_blank CHECK (btrim(name) <> ''),
    CONSTRAINT chk_templates_active_boolean CHECK (active IN ('T', 'F'))
);

CREATE UNIQUE INDEX uk_templates_code ON templates (code);
CREATE INDEX idx_templates_active ON templates (active);

INSERT INTO templates (code, name, active, created_at, updated_at)
VALUES
    ('STANDARD', 'Standard Page', 'T', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('LANDING', 'Landing Page', 'T', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('BLOG', 'Blog Layout', 'T', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

ALTER TABLE pages
    ADD COLUMN template_id BIGINT;

UPDATE pages
SET template_id = (SELECT id FROM templates WHERE code = 'STANDARD')
WHERE template_id IS NULL;

ALTER TABLE pages
    ADD CONSTRAINT fk_pages_template
        FOREIGN KEY (template_id) REFERENCES templates(id);

CREATE INDEX idx_pages_template ON pages (template_id);

CREATE TABLE site_settings (
    id BIGSERIAL PRIMARY KEY,
    site_name VARCHAR(255),
    logo_media_id BIGINT,
    footer_text TEXT,
    contact_email VARCHAR(255),
    phone VARCHAR(100),
    facebook_url VARCHAR(500),
    linkedin_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE UNIQUE INDEX uk_site_settings_singleton ON site_settings ((true));

INSERT INTO site_settings (created_at, updated_at)
SELECT CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM site_settings);
