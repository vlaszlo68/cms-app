CREATE TABLE IF NOT EXISTS menus (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(100) NOT NULL,
    active VARCHAR(1) NOT NULL DEFAULT 'T',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by BIGINT,
    updated_by BIGINT,
    CONSTRAINT chk_menus_name_not_blank CHECK (btrim(name) <> ''),
    CONSTRAINT chk_menus_code_not_blank CHECK (btrim(code) <> ''),
    CONSTRAINT chk_menus_active_boolean CHECK (active IN ('T', 'F'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_menus_code ON menus (code);

CREATE TABLE IF NOT EXISTS menu_items (
    id BIGSERIAL PRIMARY KEY,
    menu_id BIGINT NOT NULL,
    parent_id BIGINT,
    page_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    visible VARCHAR(1) NOT NULL DEFAULT 'T',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by BIGINT,
    updated_by BIGINT,
    CONSTRAINT fk_menu_items_menu
        FOREIGN KEY (menu_id) REFERENCES menus(id) ON DELETE CASCADE,
    CONSTRAINT fk_menu_items_page
        FOREIGN KEY (page_id) REFERENCES pages(id),
    CONSTRAINT fk_menu_items_parent
        FOREIGN KEY (parent_id) REFERENCES menu_items(id) ON DELETE CASCADE,
    CONSTRAINT chk_menu_items_title_not_blank CHECK (btrim(title) <> ''),
    CONSTRAINT chk_menu_items_visible_boolean CHECK (visible IN ('T', 'F'))
);

CREATE INDEX IF NOT EXISTS idx_menu_items_menu ON menu_items (menu_id);
CREATE INDEX IF NOT EXISTS idx_menu_items_parent ON menu_items (parent_id);
CREATE INDEX IF NOT EXISTS idx_menu_items_page ON menu_items (page_id);
CREATE INDEX IF NOT EXISTS idx_menu_items_order ON menu_items (menu_id, parent_id, sort_order, id);
