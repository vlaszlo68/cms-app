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

WITH owned_page AS (
    SELECT page.id
    FROM pages AS page
    JOIN templates AS template ON template.id = page.template_id
    WHERE page.slug = 'block-demo'
      AND page.title = 'AI Agent Block Demo'
      AND page.status = 'PUBLISHED'
      AND page.page_type = 'BLOCK'
      AND page.content IS NULL
      AND page.homepage = 'F'
      AND page.menu_visible = 'T'
      AND template.code = 'STANDARD'
      AND template.active = 'T'
), fingerprint(legacy_type, legacy_title, legacy_sort_order, legacy_visible, legacy_config_json,
               desired_title, desired_sort_order, desired_config_json) AS (
    VALUES
        ('HERO', 'Structured AI Coding Agents', 1, 'T',
         $legacy_hero${"headline":"Build software with structured AI coding agents","subHeadline":"Plan, implement, review and improve features through a repeatable development workflow.","buttonLabel":"Explore the workflow","buttonUrl":"/rolunk"}$legacy_hero$,
         'Structured AI Coding Agents', 1,
         $desired_hero${"headline":"Build software with structured AI coding agents","subHeadline":"Use specialized agents for planning, implementation, review and iterative fixing while keeping every feature small, testable and understandable.","buttonLabel":"Learn about the workflow","buttonUrl":"/rolunk"}$desired_hero$),
        ('TEXT', 'From Prompt to Reviewed Code', 2, 'T',
         $legacy_text${"html":"<h2>From prompt to reviewed code</h2><p>This BLOCK page is built from PageBlock records. It demonstrates how a CMS page can be composed from independent sections instead of one large HTML field.</p><p>In this demo, each block represents a part of an AI-assisted development process. The HERO block introduces the main idea, the TEXT block explains the workflow, and the CTA block guides the visitor to the next step.</p><p>This structure will make it possible to build more advanced pages later, including landing pages, galleries, feature sections and media-rich public content.</p>"}$legacy_text$,
         'A workflow instead of random code generation', 2,
         $desired_text_2${"html":"<h2>A workflow instead of random code generation</h2><p>AI-assisted development becomes much more useful when agents follow a clear software delivery process. Instead of asking one agent to generate a large change in a single step, the work is divided into exploration, planning, implementation, review and correction.</p><p>This CMS demo page is rendered from PageBlock records. Each block represents a separate content section, just like each agent role represents a separate responsibility in the development workflow.</p><ul><li>The Explorer understands the existing codebase.</li><li>The Planner creates a controlled implementation plan.</li><li>The Implementer changes the code according to the plan.</li><li>The Reviewer checks correctness, architecture and maintainability.</li><li>The Fixer applies targeted corrections when needed.</li></ul>"}$desired_text_2$),
        ('CTA', 'Deliver Smaller and Safer Features', 3, 'T',
         $legacy_cta${"title":"Ready to deliver safer features?","text":"Use specialized backend, frontend and reviewer agents to build smaller, clearer and more testable vertical slices.","buttonLabel":"Read about the workflow","buttonUrl":"/rolunk"}$legacy_cta$,
         'Continue with the workflow overview', 5,
         $desired_cta${"title":"Continue with the workflow overview","text":"Read more about how Explorer, Planner, Implementer, Reviewer and Fixer agents can work together to deliver safer fullstack features.","buttonLabel":"Open About page","buttonUrl":"/rolunk"}$desired_cta$)
)
SELECT 1 / CASE WHEN EXISTS (
    SELECT 1
    FROM owned_page AS page
    CROSS JOIN fingerprint AS seed
    JOIN page_blocks AS block ON block.page_id = page.id
        AND block.block_type = seed.legacy_type
        AND block.title = seed.legacy_title
        AND block.sort_order = seed.legacy_sort_order
        AND block.visible = seed.legacy_visible
        AND block.config_json = seed.legacy_config_json
    GROUP BY page.id, seed.legacy_type, seed.legacy_title, seed.legacy_sort_order,
             seed.legacy_visible, seed.legacy_config_json
    HAVING COUNT(*) > 1
) OR EXISTS (
    SELECT 1
    FROM owned_page AS page
    CROSS JOIN fingerprint AS seed
    WHERE EXISTS (
        SELECT 1 FROM page_blocks AS legacy
        WHERE legacy.page_id = page.id
          AND legacy.block_type = seed.legacy_type
          AND legacy.title = seed.legacy_title
          AND legacy.sort_order = seed.legacy_sort_order
          AND legacy.visible = seed.legacy_visible
          AND legacy.config_json = seed.legacy_config_json
    )
      AND EXISTS (
        SELECT 1 FROM page_blocks AS desired
        WHERE desired.page_id = page.id
          AND desired.block_type = seed.legacy_type
          AND desired.title = seed.desired_title
          AND desired.sort_order = seed.desired_sort_order
          AND desired.visible = 'T'
          AND desired.config_json = seed.desired_config_json
    )
) THEN 0 ELSE 1 END
FROM owned_page;

UPDATE page_blocks AS block
SET title = seed.desired_title,
    sort_order = seed.desired_sort_order,
    visible = 'T',
    config_json = seed.desired_config_json,
    updated_at = CURRENT_TIMESTAMP
FROM pages AS page
JOIN templates AS template ON template.id = page.template_id
CROSS JOIN (
    VALUES
        ('HERO', 'Structured AI Coding Agents', 1, 'T',
         $legacy_hero${"headline":"Build software with structured AI coding agents","subHeadline":"Plan, implement, review and improve features through a repeatable development workflow.","buttonLabel":"Explore the workflow","buttonUrl":"/rolunk"}$legacy_hero$,
         'Structured AI Coding Agents', 1,
         $desired_hero${"headline":"Build software with structured AI coding agents","subHeadline":"Use specialized agents for planning, implementation, review and iterative fixing while keeping every feature small, testable and understandable.","buttonLabel":"Learn about the workflow","buttonUrl":"/rolunk"}$desired_hero$),
        ('TEXT', 'From Prompt to Reviewed Code', 2, 'T',
         $legacy_text${"html":"<h2>From prompt to reviewed code</h2><p>This BLOCK page is built from PageBlock records. It demonstrates how a CMS page can be composed from independent sections instead of one large HTML field.</p><p>In this demo, each block represents a part of an AI-assisted development process. The HERO block introduces the main idea, the TEXT block explains the workflow, and the CTA block guides the visitor to the next step.</p><p>This structure will make it possible to build more advanced pages later, including landing pages, galleries, feature sections and media-rich public content.</p>"}$legacy_text$,
         'A workflow instead of random code generation', 2,
         $desired_text_2${"html":"<h2>A workflow instead of random code generation</h2><p>AI-assisted development becomes much more useful when agents follow a clear software delivery process. Instead of asking one agent to generate a large change in a single step, the work is divided into exploration, planning, implementation, review and correction.</p><p>This CMS demo page is rendered from PageBlock records. Each block represents a separate content section, just like each agent role represents a separate responsibility in the development workflow.</p><ul><li>The Explorer understands the existing codebase.</li><li>The Planner creates a controlled implementation plan.</li><li>The Implementer changes the code according to the plan.</li><li>The Reviewer checks correctness, architecture and maintainability.</li><li>The Fixer applies targeted corrections when needed.</li></ul>"}$desired_text_2$),
        ('CTA', 'Deliver Smaller and Safer Features', 3, 'T',
         $legacy_cta${"title":"Ready to deliver safer features?","text":"Use specialized backend, frontend and reviewer agents to build smaller, clearer and more testable vertical slices.","buttonLabel":"Read about the workflow","buttonUrl":"/rolunk"}$legacy_cta$,
         'Continue with the workflow overview', 5,
         $desired_cta${"title":"Continue with the workflow overview","text":"Read more about how Explorer, Planner, Implementer, Reviewer and Fixer agents can work together to deliver safer fullstack features.","buttonLabel":"Open About page","buttonUrl":"/rolunk"}$desired_cta$)
) AS seed(legacy_type, legacy_title, legacy_sort_order, legacy_visible, legacy_config_json,
          desired_title, desired_sort_order, desired_config_json)
WHERE block.page_id = page.id
  AND block.block_type = seed.legacy_type
  AND block.title = seed.legacy_title
  AND block.sort_order = seed.legacy_sort_order
  AND block.visible = seed.legacy_visible
  AND block.config_json = seed.legacy_config_json
  AND page.slug = 'block-demo'
  AND page.title = 'AI Agent Block Demo'
  AND page.status = 'PUBLISHED'
  AND page.page_type = 'BLOCK'
  AND page.content IS NULL
  AND page.homepage = 'F'
  AND page.menu_visible = 'T'
  AND template.code = 'STANDARD'
  AND template.active = 'T';

INSERT INTO page_blocks (page_id, block_type, title, sort_order, visible, config_json, created_at, updated_at)
SELECT page.id, seed.block_type, seed.title, seed.sort_order, 'T', seed.config_json, CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
FROM pages AS page
JOIN templates AS template ON template.id = page.template_id
CROSS JOIN (
    VALUES
        ('HERO', 'Structured AI Coding Agents', 1,
         $desired_hero${"headline":"Build software with structured AI coding agents","subHeadline":"Use specialized agents for planning, implementation, review and iterative fixing while keeping every feature small, testable and understandable.","buttonLabel":"Learn about the workflow","buttonUrl":"/rolunk"}$desired_hero$),
        ('TEXT', 'A workflow instead of random code generation', 2,
         $desired_text_2${"html":"<h2>A workflow instead of random code generation</h2><p>AI-assisted development becomes much more useful when agents follow a clear software delivery process. Instead of asking one agent to generate a large change in a single step, the work is divided into exploration, planning, implementation, review and correction.</p><p>This CMS demo page is rendered from PageBlock records. Each block represents a separate content section, just like each agent role represents a separate responsibility in the development workflow.</p><ul><li>The Explorer understands the existing codebase.</li><li>The Planner creates a controlled implementation plan.</li><li>The Implementer changes the code according to the plan.</li><li>The Reviewer checks correctness, architecture and maintainability.</li><li>The Fixer applies targeted corrections when needed.</li></ul>"}$desired_text_2$),
        ('TEXT', 'Separate agents, clear contracts', 3,
         $desired_text_3${"html":"<h2>Separate agents, clear contracts</h2><p>In a fullstack project, backend and frontend work usually require different context. A backend agent can focus on Java services, database queries, public API endpoints and response DTOs. A frontend-public agent can focus on routing, rendering, user experience and browser-level end-to-end behavior.</p><p>The connection between these agents is the integration contract. It describes endpoint URLs, HTTP methods, response fields, error behavior and authentication rules. When this contract is clear, the frontend can safely build on the backend without guessing how the API is supposed to behave.</p><p>This approach is especially useful for vertical-slice development, where every feature should move the product toward a complete user-visible behavior instead of producing disconnected technical layers.</p>"}$desired_text_3$),
        ('TEXT', 'Review-driven iteration', 4,
         $desired_text_4${"html":"<h2>Review-driven iteration</h2><p>The reviewer role is important because generated code still needs engineering judgment. A useful review does not only check whether the code compiles. It also checks whether the solution fits the existing architecture, preserves security, avoids unnecessary complexity and remains testable.</p><p>If the reviewer finds blocking issues, the fixer receives the review report and corrects only the reported problems. This keeps the workflow focused and prevents accidental scope expansion. The final reviewer then verifies that the correction really solved the issue and that no new regression was introduced.</p><p>The result is a more disciplined way to use AI coding agents: not as uncontrolled code generators, but as participants in a structured development process.</p>"}$desired_text_4$),
        ('CTA', 'Continue with the workflow overview', 5,
         $desired_cta${"title":"Continue with the workflow overview","text":"Read more about how Explorer, Planner, Implementer, Reviewer and Fixer agents can work together to deliver safer fullstack features.","buttonLabel":"Open About page","buttonUrl":"/rolunk"}$desired_cta$)
) AS seed(block_type, title, sort_order, config_json)
WHERE page.slug = 'block-demo'
  AND page.title = 'AI Agent Block Demo'
  AND page.status = 'PUBLISHED'
  AND page.page_type = 'BLOCK'
  AND page.content IS NULL
  AND page.homepage = 'F'
  AND page.menu_visible = 'T'
  AND template.code = 'STANDARD'
  AND template.active = 'T'
  AND NOT EXISTS (
      SELECT 1
      FROM page_blocks AS block
      WHERE block.page_id = page.id
        AND block.block_type = seed.block_type
        AND block.title = seed.title
        AND block.sort_order = seed.sort_order
        AND block.visible = 'T'
        AND block.config_json = seed.config_json
  );
