CREATE TABLE applications (
    id BIGSERIAL PRIMARY KEY,
    site_id BIGINT NOT NULL REFERENCES sites(id) ON DELETE RESTRICT,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    active VARCHAR(1) NOT NULL DEFAULT 'T',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by BIGINT,
    updated_by BIGINT,
    CONSTRAINT chk_applications_code_not_blank CHECK (btrim(code) <> ''),
    CONSTRAINT chk_applications_name_not_blank CHECK (btrim(name) <> ''),
    CONSTRAINT chk_applications_active_boolean CHECK (active IN ('T', 'F')),
    CONSTRAINT uk_applications_site_code UNIQUE (site_id, code)
);
CREATE INDEX idx_applications_site ON applications (site_id);
ALTER TABLE menu_items ADD COLUMN application_id BIGINT;
ALTER TABLE menu_items ADD CONSTRAINT fk_menu_items_application FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE RESTRICT;
ALTER TABLE menu_items DROP CONSTRAINT IF EXISTS chk_menu_items_target_type;
ALTER TABLE menu_items ADD CONSTRAINT chk_menu_items_target_type CHECK (target_type IN ('PAGE', 'URL', 'APPLICATION'));
