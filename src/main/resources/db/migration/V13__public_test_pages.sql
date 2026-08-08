SELECT 1 / (
    SELECT CASE
        WHEN EXISTS (
            SELECT 1
            FROM templates
            WHERE code = 'STANDARD'
              AND active = 'T'
        ) THEN 1
        ELSE 0
    END
);

UPDATE pages
SET status = 'PUBLISHED',
    page_type = 'CONTENT',
    template_id = (
        SELECT id
        FROM templates
        WHERE code = 'STANDARD'
          AND active = 'T'
    ),
    content = CASE
        WHEN content IS NULL OR btrim(content) = '' THEN '<p>Welcome to the public test home page.</p>'
        ELSE content
    END,
    updated_at = CURRENT_TIMESTAMP
WHERE slug = 'home';

UPDATE pages
SET status = 'PUBLISHED',
    page_type = 'CONTENT',
    template_id = (
        SELECT id
        FROM templates
        WHERE code = 'STANDARD'
          AND active = 'T'
    ),
    content = CASE
        WHEN content IS NULL OR btrim(content) = '' THEN '<p>Rólunk: public test content.</p>'
        ELSE content
    END,
    updated_at = CURRENT_TIMESTAMP
WHERE slug = 'rolunk';

INSERT INTO pages (title, slug, content, status, template_id, page_type, homepage, menu_visible, created_at, updated_at)
SELECT 'Home', 'home', '<p>Welcome to the public test home page.</p>', 'PUBLISHED', id, 'CONTENT', 'F', 'T', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM templates
WHERE code = 'STANDARD'
  AND active = 'T'
  AND NOT EXISTS (SELECT 1 FROM pages WHERE slug = 'home')
ON CONFLICT (slug) DO NOTHING;

INSERT INTO pages (title, slug, content, status, template_id, page_type, homepage, menu_visible, created_at, updated_at)
SELECT 'Rólunk', 'rolunk', '<p>Rólunk: public test content.</p>', 'PUBLISHED', id, 'CONTENT', 'F', 'T', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM templates
WHERE code = 'STANDARD'
  AND active = 'T'
  AND NOT EXISTS (SELECT 1 FROM pages WHERE slug = 'rolunk')
ON CONFLICT (slug) DO NOTHING;
