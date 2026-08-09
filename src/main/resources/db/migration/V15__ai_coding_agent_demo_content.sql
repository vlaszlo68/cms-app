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
SET title = 'AI Coding Workflow Hub',
    content = $home_html$<h1>AI Coding Workflow Hub</h1>
<p>AI-assisted software development works best when coding agents do not simply generate random code, but follow a structured workflow. This demo CMS site presents a development model where specialized agents help with exploration, planning, implementation, review and iterative correction.</p>
<p>The goal is to show how backend, frontend-admin and frontend-public agents can collaborate on one product while still working in separate codebases and contexts. Each agent has a clear responsibility, and every larger feature is designed as a vertical slice that can be tested through a real user flow.</p>
<h2>What this demo site demonstrates</h2>
<p>This CMS is used to demonstrate how content, menus, templates and page blocks can be managed from an admin interface and rendered on a public website. The sample content focuses on AI coding agents, integration contracts, review-driven development and safer incremental delivery.</p>
<ul><li>Structured planning before implementation</li><li>Separate backend, frontend-admin and frontend-public agent responsibilities</li><li>Review and fixer iterations before a feature is considered complete</li><li>Vertical-slice delivery instead of disconnected technical layers</li><li>Public rendering of CMS-managed pages and blocks</li></ul>
<p>This page is a CONTENT page. It is rendered directly from the Page.content field and is useful for validating the basic public page rendering pipeline.</p>$home_html$,
    status = 'PUBLISHED',
    page_type = 'CONTENT',
    template_id = (SELECT id FROM templates WHERE code = 'STANDARD' AND active = 'T'),
    homepage = 'F',
    menu_visible = 'T',
    updated_at = CURRENT_TIMESTAMP
WHERE slug = 'home'
  AND (title IS NULL
       OR btrim(title) = ''
       OR title = 'Home')
  AND (content IS NULL
       OR btrim(content) = ''
       OR content = '<p>Welcome to the public test home page.</p>');

UPDATE pages
SET title = 'About the AI Agent Workflow',
    content = $rolunk_prefix$<h1>About the AI Agent Workflow</h1>
<p>This demo website describes a structured software development workflow supported by AI coding agents. Instead of asking one agent to do everything at once, the work is divided into clear roles: Explorer, Planner, Implementer, Reviewer, Fixer, Final Reviewer and Summary.</p>
<p>The Explorer understands the existing codebase and identifies architectural constraints. The Planner creates an implementation plan, integration contract, acceptance criteria and test strategy. The Implementer follows the plan and changes only what is needed. The Reviewer checks correctness, architecture, security, maintainability and E2E testability. If problems are found, the Fixer corrects them and the Final Reviewer verifies the result.</p>
<h2>Why separate agents?</h2>
<p>In a fullstack project, backend and frontend work often require different context. A backend agent can make an API backend-ready and document the integration contract. A frontend-public agent can then use that contract to complete the browser-level vertical slice. A frontend-admin agent can focus on the CMS management experience.</p>
<h2>Why vertical slices?</h2>
<p>A feature is most valuable when it can be tested as a real user flow. For example, a CMS page should not only exist in the database$rolunk_prefix$ || chr(59) || $rolunk_suffix$ it should be available through the public API and visible in the browser. This approach keeps development safer, more understandable and easier to review.</p>
<p>This page is also a CONTENT page and should continue to be useful for validating ordinary HTML content rendering on the public frontend.</p>$rolunk_suffix$,
    status = 'PUBLISHED',
    page_type = 'CONTENT',
    template_id = (SELECT id FROM templates WHERE code = 'STANDARD' AND active = 'T'),
    homepage = 'F',
    menu_visible = 'T',
    updated_at = CURRENT_TIMESTAMP
WHERE slug = 'rolunk'
  AND (title IS NULL
       OR btrim(title) = ''
       OR title = 'Rólunk')
  AND (content IS NULL
       OR btrim(content) = ''
       OR content = '<p>Rólunk: public test content.</p>');

INSERT INTO pages (title, slug, content, status, template_id, page_type, homepage, menu_visible, created_at, updated_at)
SELECT 'AI Coding Workflow Hub', 'home', $home_html$<h1>AI Coding Workflow Hub</h1>
<p>AI-assisted software development works best when coding agents do not simply generate random code, but follow a structured workflow. This demo CMS site presents a development model where specialized agents help with exploration, planning, implementation, review and iterative correction.</p>
<p>The goal is to show how backend, frontend-admin and frontend-public agents can collaborate on one product while still working in separate codebases and contexts. Each agent has a clear responsibility, and every larger feature is designed as a vertical slice that can be tested through a real user flow.</p>
<h2>What this demo site demonstrates</h2>
<p>This CMS is used to demonstrate how content, menus, templates and page blocks can be managed from an admin interface and rendered on a public website. The sample content focuses on AI coding agents, integration contracts, review-driven development and safer incremental delivery.</p>
<ul><li>Structured planning before implementation</li><li>Separate backend, frontend-admin and frontend-public agent responsibilities</li><li>Review and fixer iterations before a feature is considered complete</li><li>Vertical-slice delivery instead of disconnected technical layers</li><li>Public rendering of CMS-managed pages and blocks</li></ul>
<p>This page is a CONTENT page. It is rendered directly from the Page.content field and is useful for validating the basic public page rendering pipeline.</p>$home_html$, 'PUBLISHED', id, 'CONTENT', 'F', 'T', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM templates
WHERE code = 'STANDARD'
  AND active = 'T'
  AND NOT EXISTS (SELECT 1 FROM pages WHERE slug = 'home');

INSERT INTO pages (title, slug, content, status, template_id, page_type, homepage, menu_visible, created_at, updated_at)
SELECT 'About the AI Agent Workflow', 'rolunk', $rolunk_prefix$<h1>About the AI Agent Workflow</h1>
<p>This demo website describes a structured software development workflow supported by AI coding agents. Instead of asking one agent to do everything at once, the work is divided into clear roles: Explorer, Planner, Implementer, Reviewer, Fixer, Final Reviewer and Summary.</p>
<p>The Explorer understands the existing codebase and identifies architectural constraints. The Planner creates an implementation plan, integration contract, acceptance criteria and test strategy. The Implementer follows the plan and changes only what is needed. The Reviewer checks correctness, architecture, security, maintainability and E2E testability. If problems are found, the Fixer corrects them and the Final Reviewer verifies the result.</p>
<h2>Why separate agents?</h2>
<p>In a fullstack project, backend and frontend work often require different context. A backend agent can make an API backend-ready and document the integration contract. A frontend-public agent can then use that contract to complete the browser-level vertical slice. A frontend-admin agent can focus on the CMS management experience.</p>
<h2>Why vertical slices?</h2>
<p>A feature is most valuable when it can be tested as a real user flow. For example, a CMS page should not only exist in the database$rolunk_prefix$ || chr(59) || $rolunk_suffix$ it should be available through the public API and visible in the browser. This approach keeps development safer, more understandable and easier to review.</p>
<p>This page is also a CONTENT page and should continue to be useful for validating ordinary HTML content rendering on the public frontend.</p>$rolunk_suffix$, 'PUBLISHED', id, 'CONTENT', 'F', 'T', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM templates
WHERE code = 'STANDARD'
  AND active = 'T'
  AND NOT EXISTS (SELECT 1 FROM pages WHERE slug = 'rolunk');

UPDATE pages
SET title = 'AI Agent Block Demo',
    content = NULL,
    status = 'PUBLISHED',
    template_id = (SELECT id FROM templates WHERE code = 'STANDARD' AND active = 'T'),
    page_type = 'BLOCK',
    homepage = 'F',
    menu_visible = 'T',
    updated_at = CURRENT_TIMESTAMP
WHERE slug = 'block-demo'
  AND title = 'AI Agent Block Demo'
  AND status = 'PUBLISHED'
  AND page_type = 'BLOCK'
  AND template_id = (SELECT id FROM templates WHERE code = 'STANDARD' AND active = 'T')
  AND (content IS NULL OR btrim(content) = '');

INSERT INTO pages (title, slug, content, status, template_id, page_type, homepage, menu_visible, created_at, updated_at)
SELECT 'AI Agent Block Demo', 'block-demo', NULL, 'PUBLISHED', id, 'BLOCK', 'F', 'T', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM templates
WHERE code = 'STANDARD'
  AND active = 'T'
  AND NOT EXISTS (SELECT 1 FROM pages WHERE slug = 'block-demo');

INSERT INTO page_blocks (page_id, block_type, title, sort_order, visible, config_json, created_at, updated_at)
SELECT page.id, seed.block_type, seed.title, seed.sort_order, 'T', seed.config_json, CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
FROM pages page
CROSS JOIN (
    VALUES
        ('HERO', 'Structured AI Coding Agents', 1,
         $hero_json${"headline":"Build software with structured AI coding agents","subHeadline":"Plan, implement, review and improve features through a repeatable development workflow.","buttonLabel":"Explore the workflow","buttonUrl":"/rolunk"}$hero_json$),
        ('TEXT', 'From Prompt to Reviewed Code', 2,
         $text_json${"html":"<h2>From prompt to reviewed code</h2><p>This BLOCK page is built from PageBlock records. It demonstrates how a CMS page can be composed from independent sections instead of one large HTML field.</p><p>In this demo, each block represents a part of an AI-assisted development process. The HERO block introduces the main idea, the TEXT block explains the workflow, and the CTA block guides the visitor to the next step.</p><p>This structure will make it possible to build more advanced pages later, including landing pages, galleries, feature sections and media-rich public content.</p>"}$text_json$),
        ('CTA', 'Deliver Smaller and Safer Features', 3,
         $cta_json${"title":"Ready to deliver safer features?","text":"Use specialized backend, frontend and reviewer agents to build smaller, clearer and more testable vertical slices.","buttonLabel":"Read about the workflow","buttonUrl":"/rolunk"}$cta_json$)
) AS seed(block_type, title, sort_order, config_json)
WHERE page.slug = 'block-demo'
  AND page.title = 'AI Agent Block Demo'
  AND page.status = 'PUBLISHED'
  AND page.page_type = 'BLOCK'
  AND page.template_id = (SELECT id FROM templates WHERE code = 'STANDARD' AND active = 'T')
  AND (page.content IS NULL OR btrim(page.content) = '')
  AND NOT EXISTS (
      SELECT 1
      FROM page_blocks block
      WHERE block.page_id = page.id
        AND block.block_type = seed.block_type
  );
