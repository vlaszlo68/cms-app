# CMS Project Context

## Overview

Custom-built Content Management System (CMS) with a Java backend and React frontend.

Goal: clean, minimal, framework-light architecture with full control over implementation.

---

## Tech Stack

### Backend

- Java 21 (Temurin)
- Servlet API (no Spring Boot)
- JDBC (no ORM / Hibernate)
- PostgreSQL
- HikariCP
- Gson
- SLF4J + Logback
- Maven
- WAR packaging
- Deployment target: Tomcat 9

### Frontend

- React
- Separate repository
- Communicates via REST API

### DevOps / Local Infrastructure

- Docker Compose
- PostgreSQL 15 container
- Tomcat container
- Jenkins container

---

## Architecture

Strict 3-layer backend:

1. DAO
   - SQL only
   - PreparedStatement
   - No business logic

2. Service
   - Business logic
   - Validation
   - Transaction-aware business workflows

3. Servlet
   - HTTP handling
   - Session management
   - JSON input/output

Cross-cutting HTTP behavior is handled by servlet filters:

- exception-to-JSON handling
- request logging
- CORS
- no-store/security headers
- UTF-8 request/response encoding
- authentication
- CSRF validation
- request-scoped DB transaction handling

---

## API

- REST + JSON
- Base path: `/api`
- Common API response envelope:
  - success: `{ "success": true, "data": ... }`
  - error: `{ "success": false, "error": { "code": "...", "message": "..." } }`

Examples:

- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/pages`
- `POST /api/pages`
- `PUT /api/pages/{id}`
- `DELETE /api/pages/{id}`

Current implementation note:

- current auth endpoints are implemented under `/api/auth/*`
  - `POST /api/auth/login`
  - `POST /api/auth/logout`
  - `GET /api/auth/me`
- auth endpoints now use the common API response envelope
- successful auth responses include a CSRF token in `data.csrfToken`
- local standalone Tomcat deploy path in current development is:
  - `http://localhost:8081/cms-app/api/auth/login`
  - `http://localhost:8081/cms-app/api/auth/logout`
  - `http://localhost:8081/cms-app/api/auth/me`
- Docker Tomcat deploy path in current development is:
  - `http://localhost:8081/api/auth/login`
  - `http://localhost:8081/api/auth/logout`
  - `http://localhost:8081/api/auth/me`
- the current DB connection code reads `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` first, then falls back to `web.xml` context params and finally hardcoded defaults
- DB-backed DAO tests run during `mvn test` / `mvn package`, so CI environments must provide these DB environment variables or otherwise make PostgreSQL reachable

---

## Authentication

- Session-based
- Cookie handled by browser
- No JWT
- CSRF protection is active for state-changing `/api/*` requests
- Frontend must send `X-CSRF-Token` on `POST`, `PUT`, `PATCH`, and `DELETE` after login/session restore

---

## Database

- PostgreSQL
- Manual SQL (JDBC)
- Connection pool: HikariCP
- Request-scoped transactions through `TransactionFilter` and `TransactionContext`
- Prefer ANSI SQL where possible
- Use vendor-specific SQL only when justified
- Current connection entry point: `hu.laci.cms.backend.config.database.DatabaseConfig`

DAO layer notes:

- generic CRUD support lives in `hu.laci.cms.backend.dao.common.BaseDao`
- DAO IDs are `Long`
- generated `SELECT` SQL uses table-qualified columns with aliases, for example `users.id AS users_id`
- `findAll` supports multiple sort fields
- default sort is `id ASC`
- `LIKE` filters add `%` in the base DAO according to annotation configuration
- Java `Boolean`/`boolean` maps to database `VARCHAR(1)` values `T`/`F`
- DB mapping annotations live under `hu.laci.cms.backend.dao.common.annotations`
- filter annotations live under `hu.laci.cms.backend.model.common.annotations`

---

## Current Structure

cms/
|-- src/
|   |-- main/
|   |   |-- java/
|   |   |   `-- hu/laci/cms/
|   |   |       |-- Main.java
|   |   |       `-- backend/
|   |   |           |-- config/database/
|   |   |           |-- dao/
|   |   |           |-- dto/auth/
|   |   |           |-- dto/common/
|   |   |           |-- model/
|   |   |           |-- service/
|   |   |           `-- servlet/
|   |   `-- webapp/
|-- docker/
|   |-- postgres/
|   `-- tomcat/
|-- skills/
|-- FRONTEND_BOOTSTRAP_PLAN.md
|-- FRONTEND_HANDOFF.md
|-- SESSION_CONTEXT.md
|-- docker-compose.yml
|-- pom.xml
|-- project.md
`-- agent.md

---

## Target Backend Structure

- `hu.laci.cms.backend.model`
- `hu.laci.cms.backend.dao`
- `hu.laci.cms.backend.service`
- `hu.laci.cms.backend.servlet`
- `hu.laci.cms.backend.dto`
- `hu.laci.cms.backend.config`

---

## Development Principles

- No frameworks (no Spring)
- No ORM
- Keep code simple and explicit
- Respect layer boundaries
- Avoid unnecessary abstractions

---

## Current Backend Status

- `User`, `UserDao`, `UserDaoImpl`, `AuthService`, `AuthServiceException`, `DatabaseConfig` already exist
- generic DAO CRUD and filtering/sorting support are implemented in `BaseDao`
- DAO integration tests currently cover user DAO behavior, CRUD, transaction commit/rollback, boolean mapping, filtering, sorting, and save/create/update/delete
- auth servlet layer is now implemented with:
  - `hu.laci.cms.backend.servlet.auth.AuthServlet`
  - `hu.laci.cms.backend.servlet.auth.LogoutServlet`
  - `hu.laci.cms.backend.servlet.auth.MeServlet`
  - `hu.laci.cms.backend.dto.auth.LoginRequest`
  - `hu.laci.cms.backend.dto.auth.AuthenticatedUser`
  - `hu.laci.cms.backend.dto.common.ApiResponse`
  - `hu.laci.cms.backend.dto.common.ApiErrorResponse`
  - `hu.laci.cms.backend.servlet.support.JsonServletSupport`
  - `hu.laci.cms.backend.servlet.filter.AuthFilter`
  - `hu.laci.cms.backend.servlet.filter.ExceptionHandlingFilter`
  - `hu.laci.cms.backend.servlet.filter.RequestLoggingFilter`
  - `hu.laci.cms.backend.servlet.filter.CorsFilter`
  - `hu.laci.cms.backend.servlet.filter.SecurityHeadersFilter`
  - `hu.laci.cms.backend.servlet.filter.CharacterEncodingFilter`
  - `hu.laci.cms.backend.servlet.filter.CsrfFilter`
  - `hu.laci.cms.backend.servlet.health.HelloServlet`
  - `hu.laci.cms.backend.servlet.support.CsrfTokenSupport`
- JSON request/response handling currently uses Gson
- successful JSON API responses are wrapped as `success/data`
- JSON API errors are wrapped as `success/error.code/error.message`
- session-based authentication is active through `HttpSession`
- successful login rotates the session id before storing auth state
- the session stores `AuthenticatedUser`, not the full persistence `User`
- successful login creates a session CSRF token
- `POST /api/auth/login` and `GET /api/auth/me` return `csrfToken`
- `AuthFilter` uses `request.getServletPath()`, so public auth endpoints work both under root context and `/cms-app`
- a frontend handoff and bootstrap planning documents are maintained in this repo and were copied into the separate frontend repo for frontend-side work

Current filter order in `web.xml`:

1. `exceptionHandlingFilter`
2. `requestLoggingFilter`
3. `corsFilter`
4. `securityHeadersFilter`
5. `characterEncodingFilter`
6. `authFilter`
7. `csrfFilter`
8. `transactionFilter`

---

## Current DevOps Status

- `docker-compose.yml` exists in the project root
- `docker/tomcat/Dockerfile` exists for WAR-based Tomcat image build
- current compose design includes:
  - `postgres`
  - `tomcat`
  - `jenkins`

Note:

- the compose file already passes DB environment variables to Tomcat
- the compose file passes DB environment variables to Jenkins as well, so DB-backed tests can reach PostgreSQL during CI builds
- the current Java backend configuration already supports those environment variables
- `web.xml` still contains the Docker-oriented fallback JDBC host (`postgres`), so local non-Docker Tomcat runs need `DB_HOST=localhost` override
- compose network is explicitly named `cms-network`

---

## Working Notes

- `SESSION_CONTEXT.md` stores the latest implementation summary and local runtime state for follow-up sessions
- `FRONTEND_HANDOFF.md` stores the backend contract for the separate React frontend repo
- `FRONTEND_BOOTSTRAP_PLAN.md` stores the recommended frontend repo bootstrap plan
- keep `project.md` focused on stable project context and intended architecture
- keep machine-specific or temporary setup details out of this file unless they become permanent project conventions

---

## Workflow

- Work in small steps
- Implement feature parts separately:
  - model -> DAO -> service -> servlet
- Avoid large one-step implementations
