CREATE TABLE IF NOT EXISTS pages (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    content TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    meta_title VARCHAR(255),
    meta_description VARCHAR(500),
    homepage VARCHAR(1) NOT NULL DEFAULT 'F',
    menu_visible VARCHAR(1) NOT NULL DEFAULT 'T',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by BIGINT,
    updated_by BIGINT,
    CONSTRAINT chk_pages_title_not_blank CHECK (btrim(title) <> ''),
    CONSTRAINT chk_pages_slug_not_blank CHECK (btrim(slug) <> ''),
    CONSTRAINT chk_pages_content_not_blank CHECK (btrim(content) <> ''),
    CONSTRAINT chk_pages_homepage_boolean CHECK (homepage IN ('T', 'F')),
    CONSTRAINT chk_pages_menu_visible_boolean CHECK (menu_visible IN ('T', 'F'))
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_pages_slug ON pages (slug);

CREATE INDEX IF NOT EXISTS idx_pages_status ON pages (status);

CREATE INDEX IF NOT EXISTS idx_pages_homepage ON pages (homepage);
