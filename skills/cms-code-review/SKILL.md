---
name: cms-code-review
description: Review code changes in this CMS project with focus on bugs, regressions, unsafe assumptions, layer violations, SQL/JDBC issues, and missing validation or tests. Use when Codex needs to review Java 21 Servlet + JDBC backend changes, especially in hu.laci.cms model/dao/service/servlet/config packages.
---

# CMS Code Review

Review changes with a findings-first mindset.

Report concrete issues before any summary.

Order findings by severity.

Reference exact files and lines when possible.

Focus on behavior, correctness, and maintainability risks, not style-only comments.

## Review Priorities

Check for functional bugs.

Check for behavioral regressions.

Check for broken contracts between DAO, service, servlet, and model layers.

Check for missing validation, error handling, or transaction boundaries.

Check for missing or weak test coverage when a change is risky.

## Project-Specific Checks

Stay aligned with the project structure under `src/main/java/hu/laci/cms/`.

Verify package placement is consistent with:
- `hu.laci.cms.model`
- `hu.laci.cms.dao`
- `hu.laci.cms.service`
- `hu.laci.cms.servlet`
- `hu.laci.cms.backend.config`

Enforce the 3-layer backend split:
- DAO: SQL only, `PreparedStatement`, no business logic
- Service: business rules, validation, transaction handling
- Servlet: HTTP/session/JSON handling only

Flag review findings when code:
- mixes SQL with servlet or service code without a DAO boundary
- puts business logic into DAO classes
- leaks HTTP concerns into service or DAO layers
- introduces frameworks such as Spring, ORM usage, or Lombok

## JDBC And SQL Checks

Prefer ANSI SQL where possible.

Flag PostgreSQL-specific SQL if there is no clear need for it.

Verify `PreparedStatement` is used for external input.

Verify `try-with-resources` is used for JDBC resources.

Verify connections come from `hu.laci.cms.backend.config.DatabaseConfig`.

Verify `SQLException` is wrapped into the project runtime DAO exception where applicable.

Check result-set mapping for:
- wrong column names
- incomplete field mapping
- null-handling mistakes
- type mismatches

## API And Servlet Checks

Check request parsing and response writing for correctness.

Check session handling for broken authentication or authorization behavior.

Check that servlets do not absorb business logic that belongs in services.

Check that endpoint behavior remains consistent with existing API expectations.

## Output Format

Return findings first.

For each finding, include:
- severity
- file reference
- concise explanation of the risk or bug
- expected correction direction

If no findings exist, say so explicitly.

After findings, optionally note:
- open questions
- residual risks
- missing tests
