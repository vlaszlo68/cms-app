CREATE INDEX idx_content_items_public_visibility
    ON content_items (site_id, type, status, published_at DESC, id DESC);
