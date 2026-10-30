ALTER TABLE menu_items DROP CONSTRAINT IF EXISTS fk_menu_items_application;
ALTER TABLE applications RENAME TO site_applications;
ALTER TABLE site_applications RENAME COLUMN code TO application_code;
ALTER TABLE menu_items RENAME COLUMN application_id TO site_application_id;
ALTER TABLE site_applications RENAME CONSTRAINT uk_applications_site_code TO uk_site_applications_site_application_code;
ALTER INDEX idx_applications_site RENAME TO idx_site_applications_site;
