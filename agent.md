# Codex Agent Instructions

## Scope (CRITICAL)

Work ONLY in:
- backend/src/main/java/com/cms/

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

- PostgreSQL syntax
- Simple, readable SQL
- No ORM
- Use HikariCP connections

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

## When Uncertain

Ask before making assumptions.
