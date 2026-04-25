# Codex Agent Instructions

## Scope (CRITICAL)

Edit ONLY in:
- src/main/java/hu/laci/cms/
- src/main/webapp/

You may also read project instruction files from:
- agent.md
- project.md
- skills/

Primary backend packages:
- hu.laci.cms.model
- hu.laci.cms.dao
- hu.laci.cms.service
- hu.laci.cms.servlet
- hu.laci.cms.backend.config

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
   - Transactions

3. Servlet
   - HTTP + JSON
   - Session handling

---

## Implementation Order

When building a feature:

1. Create model (if needed)
2. Create DAO
3. Create Service
4. Create Servlet
5. Map endpoint with annotations

---

## Database Rules

- Prefer ANSI SQL where possible
- Use PostgreSQL-specific syntax only when there is a clear need
- Simple, readable SQL
- No ORM
- Use HikariCP connections
- Use `hu.laci.cms.backend.config.DatabaseConfig#getConnection()`

---

## Constraints

Do NOT:
- scan entire repository
- refactor unrelated code
- rename existing classes
- introduce frameworks
- modify frontend unless asked

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
