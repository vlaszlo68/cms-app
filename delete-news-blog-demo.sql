-- Run manually against the local cms_db only.
BEGIN;
DELETE FROM content_item_tags WHERE content_item_id IN (SELECT content_item_id FROM demo_content_seed_entries d JOIN sites s ON s.id=d.site_id WHERE s.code='default');
DELETE FROM content_items WHERE id IN (SELECT content_item_id FROM demo_content_seed_entries d JOIN sites s ON s.id=d.site_id WHERE s.code='default');
DELETE FROM tags t WHERE t.site_id=(SELECT id FROM sites WHERE code='default') AND NOT EXISTS (SELECT 1 FROM content_item_tags ct WHERE ct.tag_id=t.id);
DELETE FROM demo_content_seed_entries WHERE site_id=(SELECT id FROM sites WHERE code='default');
COMMIT;
BEGIN;
DELETE FROM menu_items WHERE page_id IN (SELECT p.id FROM pages p JOIN sites s ON s.id=p.site_id WHERE s.code='default' AND p.slug IN ('news','blog')) AND title IN ('News','Blog');
DELETE FROM page_blocks WHERE page_id IN (SELECT p.id FROM pages p JOIN sites s ON s.id=p.site_id WHERE s.code='default' AND p.slug IN ('news','blog')) AND block_type='CONTENT_COLLECTION';
DELETE FROM pages WHERE site_id=(SELECT id FROM sites WHERE code='default') AND slug IN ('news','blog');
DELETE FROM content_collections WHERE site_id=(SELECT id FROM sites WHERE code='default') AND code IN ('DEMO_LATEST_NEWS','DEMO_AI_AGENT_BLOG');
COMMIT;
