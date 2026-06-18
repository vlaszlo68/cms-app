# Codex Agent Instructions

## Scope (CRITICAL)

Edit ONLY in:
- src/main/java/hu/laci/cms/backend/
- src/main/java/hu/laci/cms/Main.java
- src/main/resources/
- src/main/webapp/

You may also read project instruction files from:
- agent.md
- project.md
- SESSION_CONTEXT.md
- skills/

Primary backend packages:
- hu.laci.cms.backend.model
- hu.laci.cms.backend.dao
- hu.laci.cms.backend.service
- hu.laci.cms.backend.servlet
- hu.laci.cms.backend.dto
- hu.laci.cms.backend.config

Current implemented auth HTTP entrypoints may differ from planned API routes in `project.md`.
When continuing existing work, prefer actual current code and `SESSION_CONTEXT.md`, then align to planned routes only when explicitly requested.

Current backend implementation snapshot:
- Menu module: PAGE/URL target support, public tree endpoint, default MAIN and FOOTER menus.
- Template and Site Settings modules: configuration-only templates, singleton site settings, page template selection.
- PageType and PageBlock modules: CONTENT/BLOCK pages, ordered page blocks, optional includeBlocks response.
- Latest verified test count: 123, with the backend currently stopped unless a turn explicitly redeploys it.

Ignore:
- frontend/
- docker/
- build output (target/)
- node_modules/

---

## Architecture Rules

Always follow 3-layer structure:

1. DAO
   - SQL only
   - PreparedStatement
   - No logic

2. Service
   - Business logic
   - Validation
   - Transaction-aware workflows

3. Servlet
   - HTTP + JSON
   - Session handling

Cross-cutting HTTP concerns are implemented with servlet filters, including request logging, exception handling, CORS, security headers, UTF-8 encoding, auth, CSRF, and request-scoped transactions.

---

## Implementation Order

When building a feature:

1. Create model (if needed)
2. Create DAO
3. Create Service
4. Create Servlet
5. Map endpoint with annotations or `web.xml`, following the existing local pattern

---

## Database Rules

- Prefer ANSI SQL where possible
- Use PostgreSQL-specific syntax only when there is a clear need
- Simple, readable SQL
- No ORM
- Use HikariCP connections
- Database schema changes belong in versioned SQL files under `src/main/resources/db/migration/`
- Do not reintroduce one-off schema management through `docker/postgres/init.sql`
- DAO code should use `hu.laci.cms.backend.config.database.TransactionContext#openConnection()` so request-scoped transactions are respected
- Prefer the existing `BaseDao` + `QuerySpec` path for normal CRUD/list/filter/sort/join queries
- Use `BaseDao` custom SQL helpers from DAO subclasses for projections, aggregations, optimized SQL, or custom insert/update/delete operations that do not fit the generic query builder
- Keep old annotation-style filter classes out of new code; use entity property constants with `QuerySpec`

---

## Javadoc Rules

- Write Javadoc as part of creating new API, not as a separate cleanup step before commit.
- Every new class should have class-level Javadoc explaining its role, layer, and important lifecycle or usage constraints.
- Every new public or protected method should have Javadoc when it is part of the usable API or subclass extension surface.
- Public/protected DAO, query, transaction, servlet support, and infrastructure APIs should include parameters, return values, exceptions where relevant, and a short example when usage is not obvious.
- Private methods do not need Javadoc by default. Document private methods only when they contain non-trivial logic, important invariants, or decisions that are hard to infer from names and code.
- DTO getters/setters and standard servlet/filter/listener overrides may use short, basic Javadoc.
- Before finishing a change, check that newly added public/protected classes and methods are documented consistently.

---

## Constraints

Do NOT:
- scan entire repository
- refactor unrelated code
- rename existing classes
- introduce frameworks
- modify frontend unless asked
- introduce `var`; use explicit Java types in production and test code

---

## Performance / Usage Optimization

- Read only relevant files
- Avoid unnecessary planning steps
- Do not explore unused directories
- Keep changes minimal and targeted

---

## Output Style

- Prefer working code over explanation
- Keep explanations short
- Avoid repeating context

---

## Code Review

If the user asks for `cms-review`, also read and apply:
- skills/cms-code-review/SKILL.md

Use that file as the project-specific review checklist in addition to the rules in this file.

---

## When Uncertain

Ask before making assumptions.

## Session Continuity

If present, read `SESSION_CONTEXT.md` at the start of a new session to recover the latest implementation state, local setup notes, and verified runtime behavior.
