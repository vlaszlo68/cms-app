CREATE TABLE sites (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    active VARCHAR(1) NOT NULL DEFAULT 'T',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by BIGINT,
    updated_by BIGINT,
    CONSTRAINT chk_sites_code_not_blank CHECK (btrim(code) <> ''),
    CONSTRAINT chk_sites_code_format CHECK (code ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
    CONSTRAINT chk_sites_name_not_blank CHECK (btrim(name) <> ''),
    CONSTRAINT chk_sites_active_boolean CHECK (active IN ('T', 'F')),
    CONSTRAINT uk_sites_code UNIQUE (code)
);

INSERT INTO sites (code, name, active, created_at, updated_at)
VALUES ('default', 'Default Site', 'T', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

ALTER TABLE pages ADD COLUMN site_id BIGINT;
ALTER TABLE menus ADD COLUMN site_id BIGINT;
ALTER TABLE site_settings ADD COLUMN site_id BIGINT;
ALTER TABLE media ADD COLUMN site_id BIGINT;

UPDATE pages SET site_id = (SELECT id FROM sites WHERE code = 'default') WHERE site_id IS NULL;
UPDATE menus SET site_id = (SELECT id FROM sites WHERE code = 'default') WHERE site_id IS NULL;
UPDATE site_settings SET site_id = (SELECT id FROM sites WHERE code = 'default') WHERE site_id IS NULL;
UPDATE media SET site_id = (SELECT id FROM sites WHERE code = 'default') WHERE site_id IS NULL;

ALTER TABLE pages ALTER COLUMN site_id SET NOT NULL;
ALTER TABLE menus ALTER COLUMN site_id SET NOT NULL;
ALTER TABLE site_settings ALTER COLUMN site_id SET NOT NULL;
ALTER TABLE media ALTER COLUMN site_id SET NOT NULL;

ALTER TABLE pages ADD CONSTRAINT fk_pages_site FOREIGN KEY (site_id) REFERENCES sites(id) ON DELETE RESTRICT;
ALTER TABLE menus ADD CONSTRAINT fk_menus_site FOREIGN KEY (site_id) REFERENCES sites(id) ON DELETE RESTRICT;
ALTER TABLE site_settings ADD CONSTRAINT fk_site_settings_site FOREIGN KEY (site_id) REFERENCES sites(id) ON DELETE RESTRICT;
ALTER TABLE media ADD CONSTRAINT fk_media_site FOREIGN KEY (site_id) REFERENCES sites(id) ON DELETE RESTRICT;

CREATE INDEX idx_pages_site ON pages (site_id);
CREATE INDEX idx_menus_site ON menus (site_id);
CREATE INDEX idx_site_settings_site ON site_settings (site_id);
CREATE INDEX idx_media_site ON media (site_id);

ALTER TABLE pages DROP CONSTRAINT IF EXISTS pages_slug_key;
DROP INDEX IF EXISTS idx_pages_slug;
CREATE UNIQUE INDEX uk_pages_site_slug ON pages (site_id, slug);

DROP INDEX IF EXISTS uk_menus_code;
CREATE UNIQUE INDEX uk_menus_site_code ON menus (site_id, code);

DROP INDEX IF EXISTS uk_site_settings_singleton;
CREATE UNIQUE INDEX uk_site_settings_site ON site_settings (site_id);
