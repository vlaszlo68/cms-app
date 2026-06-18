ALTER TABLE pages
    ADD COLUMN page_type VARCHAR(30) NOT NULL DEFAULT 'CONTENT';

ALTER TABLE pages
    ALTER COLUMN content DROP NOT NULL;

ALTER TABLE pages
    ADD CONSTRAINT chk_pages_page_type CHECK (page_type IN ('CONTENT', 'BLOCK'));

CREATE INDEX idx_pages_page_type ON pages (page_type);

CREATE TABLE page_blocks (
    id BIGSERIAL PRIMARY KEY,
    page_id BIGINT NOT NULL,
    block_type VARCHAR(50) NOT NULL,
    title VARCHAR(255),
    sort_order INTEGER NOT NULL DEFAULT 0,
    visible VARCHAR(1) NOT NULL DEFAULT 'T',
    config_json TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by BIGINT,
    updated_by BIGINT,
    CONSTRAINT fk_page_blocks_page
        FOREIGN KEY (page_id) REFERENCES pages(id) ON DELETE CASCADE,
    CONSTRAINT chk_page_blocks_block_type_not_blank CHECK (btrim(block_type) <> ''),
    CONSTRAINT chk_page_blocks_visible_boolean CHECK (visible IN ('T', 'F'))
);

CREATE INDEX idx_page_blocks_page ON page_blocks (page_id);
CREATE INDEX idx_page_blocks_order ON page_blocks (page_id, sort_order, id);
