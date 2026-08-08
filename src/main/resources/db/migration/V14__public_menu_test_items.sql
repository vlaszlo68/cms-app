-- Adds only missing fixture targets to the existing active public menus.
-- The semantic target checks preserve any pre-existing user-created equivalent item.
INSERT INTO menu_items (menu_id, parent_id, page_id, target_type, target_url, title, sort_order, visible,
                        created_at, updated_at)
SELECT menu.id, NULL, page.id, 'PAGE', NULL, 'Home', 1, 'T', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM menus menu
JOIN pages page ON page.slug = 'home'
WHERE menu.code = 'MAIN'
  AND menu.active = 'T'
  AND page.status = 'PUBLISHED'
  AND page.page_type = 'CONTENT'
  AND NOT EXISTS (
      SELECT 1
      FROM menu_items item
      WHERE item.menu_id = menu.id
        AND item.target_type = 'PAGE'
        AND item.page_id = page.id
  );

INSERT INTO menu_items (menu_id, parent_id, page_id, target_type, target_url, title, sort_order, visible,
                        created_at, updated_at)
SELECT menu.id, NULL, page.id, 'PAGE', NULL, 'Rólunk', 2, 'T', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM menus menu
JOIN pages page ON page.slug = 'rolunk'
WHERE menu.code = 'MAIN'
  AND menu.active = 'T'
  AND page.status = 'PUBLISHED'
  AND page.page_type = 'CONTENT'
  AND NOT EXISTS (
      SELECT 1
      FROM menu_items item
      WHERE item.menu_id = menu.id
        AND item.target_type = 'PAGE'
        AND item.page_id = page.id
  );

INSERT INTO menu_items (menu_id, parent_id, page_id, target_type, target_url, title, sort_order, visible,
                        created_at, updated_at)
SELECT menu.id, NULL, page.id, 'PAGE', NULL, 'Rólunk', 1, 'T', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM menus menu
JOIN pages page ON page.slug = 'rolunk'
WHERE menu.code = 'FOOTER'
  AND menu.active = 'T'
  AND page.status = 'PUBLISHED'
  AND page.page_type = 'CONTENT'
  AND NOT EXISTS (
      SELECT 1
      FROM menu_items item
      WHERE item.menu_id = menu.id
        AND item.target_type = 'PAGE'
        AND item.page_id = page.id
  );

INSERT INTO menu_items (menu_id, parent_id, page_id, target_type, target_url, title, sort_order, visible,
                        created_at, updated_at)
SELECT menu.id, NULL, NULL, 'URL', 'https://example.com', 'External Test Link', 2, 'T', CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
FROM menus menu
WHERE menu.code = 'FOOTER'
  AND menu.active = 'T'
  AND NOT EXISTS (
      SELECT 1
      FROM menu_items item
      WHERE item.menu_id = menu.id
        AND item.target_type = 'URL'
        AND item.target_url = 'https://example.com'
  );
