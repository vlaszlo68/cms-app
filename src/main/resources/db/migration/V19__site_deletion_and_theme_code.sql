ALTER TABLE pages DROP CONSTRAINT IF EXISTS fk_pages_site;
ALTER TABLE menus DROP CONSTRAINT IF EXISTS fk_menus_site;
ALTER TABLE site_settings DROP CONSTRAINT IF EXISTS fk_site_settings_site;
ALTER TABLE media DROP CONSTRAINT IF EXISTS fk_media_site;
ALTER TABLE applications DROP CONSTRAINT IF EXISTS applications_site_id_fkey;

ALTER TABLE sites ADD COLUMN theme_code VARCHAR(50);
UPDATE sites SET theme_code = 'STANDARD' WHERE theme_code IS NULL;
ALTER TABLE sites ALTER COLUMN theme_code SET DEFAULT 'STANDARD';
ALTER TABLE sites ALTER COLUMN theme_code SET NOT NULL;
