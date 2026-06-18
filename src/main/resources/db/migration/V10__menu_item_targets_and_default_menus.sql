ALTER TABLE menu_items
    ADD COLUMN IF NOT EXISTS target_type VARCHAR(30) NOT NULL DEFAULT 'PAGE';

ALTER TABLE menu_items
    ADD COLUMN IF NOT EXISTS target_url VARCHAR(1000);

ALTER TABLE menu_items
    ALTER COLUMN page_id DROP NOT NULL;

ALTER TABLE menu_items
    DROP CONSTRAINT IF EXISTS chk_menu_items_target_type;

ALTER TABLE menu_items
    ADD CONSTRAINT chk_menu_items_target_type CHECK (target_type IN ('PAGE', 'URL'));

INSERT INTO menus (name, code, active, created_at, updated_at)
VALUES ('Main Navigation', 'MAIN', 'T', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO menus (name, code, active, created_at, updated_at)
VALUES ('Footer Navigation', 'FOOTER', 'T', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;
