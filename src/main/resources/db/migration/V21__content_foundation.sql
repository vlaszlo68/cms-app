CREATE TABLE content_items (
    id BIGSERIAL PRIMARY KEY,
    site_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(100) NOT NULL,
    summary VARCHAR(1000),
    content TEXT,
    group_code VARCHAR(100),
    status VARCHAR(20) NOT NULL,
    featured_media_id BIGINT,
    published_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by BIGINT,
    updated_by BIGINT,
    CONSTRAINT chk_content_items_type CHECK (type IN ('NEWS', 'BLOG', 'GALLERY')),
    CONSTRAINT chk_content_items_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT chk_content_items_title_not_blank CHECK (btrim(title) <> ''),
    CONSTRAINT chk_content_items_slug_not_blank CHECK (btrim(slug) <> ''),
    CONSTRAINT uk_content_items_site_type_slug UNIQUE (site_id, type, slug),
    CONSTRAINT fk_content_items_featured_media FOREIGN KEY (featured_media_id) REFERENCES media(id) ON DELETE RESTRICT
);

CREATE TABLE tags (
    id BIGSERIAL PRIMARY KEY,
    site_id BIGINT NOT NULL,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by BIGINT,
    updated_by BIGINT,
    CONSTRAINT chk_tags_code_not_blank CHECK (btrim(code) <> ''),
    CONSTRAINT chk_tags_name_not_blank CHECK (btrim(name) <> ''),
    CONSTRAINT uk_tags_site_code UNIQUE (site_id, code)
);

CREATE TABLE content_item_tags (
    id BIGSERIAL PRIMARY KEY,
    content_item_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    CONSTRAINT uk_content_item_tags_item_tag UNIQUE (content_item_id, tag_id),
    CONSTRAINT fk_content_item_tags_item FOREIGN KEY (content_item_id) REFERENCES content_items(id) ON DELETE CASCADE,
    CONSTRAINT fk_content_item_tags_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE RESTRICT
);

CREATE TABLE content_collections (
    id BIGSERIAL PRIMARY KEY,
    site_id BIGINT NOT NULL,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    content_type VARCHAR(20) NOT NULL,
    group_code VARCHAR(100),
    sort VARCHAR(30) NOT NULL,
    page_size INTEGER NOT NULL DEFAULT 12,
    active VARCHAR(1) NOT NULL DEFAULT 'T',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by BIGINT,
    updated_by BIGINT,
    CONSTRAINT chk_content_collections_type CHECK (content_type IN ('NEWS', 'BLOG', 'GALLERY')),
    CONSTRAINT chk_content_collections_sort CHECK (sort IN ('PUBLISHED_DESC')),
    CONSTRAINT chk_content_collections_page_size CHECK (page_size BETWEEN 1 AND 100),
    CONSTRAINT chk_content_collections_active CHECK (active IN ('T', 'F')),
    CONSTRAINT uk_content_collections_site_code UNIQUE (site_id, code)
);

CREATE INDEX idx_content_items_site_type_status ON content_items (site_id, type, status);
CREATE INDEX idx_content_items_site_group_code ON content_items (site_id, group_code);
CREATE INDEX idx_content_items_featured_media ON content_items (featured_media_id);
CREATE INDEX idx_tags_site ON tags (site_id);
CREATE INDEX idx_content_collections_site_type ON content_collections (site_id, content_type);
