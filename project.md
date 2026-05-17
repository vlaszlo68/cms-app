# CMS Project Context

## Overview

Custom-built Content Management System (CMS) with a Java backend and React frontend.

Goal: clean, minimal, framework-light architecture with full control over implementation.

The backend is intentionally servlet/JDBC based. The project avoids Spring, ORM frameworks, and large infrastructure abstractions so the request lifecycle, transaction boundaries, SQL generation, and HTTP contract remain explicit and easy to inspect.

The frontend lives in a separate React repository. This backend repository still contains frontend handoff documents because the backend API contract is the source of truth for frontend integration.

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
- TypeScript
- Vite
- Separate repository
- Communicates via REST API
- Uses browser session cookies for authentication
- Must handle CSRF tokens returned by the backend

### DevOps / Local Infrastructure

- Docker Compose
- PostgreSQL 15 container
- Tomcat container
- Jenkins container
- Named Docker network: `cms-network`

---

## Architecture

Strict 3-layer backend:

1. DAO
   - SQL only
   - PreparedStatement
   - No business logic
   - Persistence mapping and low-level data access errors only

2. Service
   - Business logic
   - Validation
   - Transaction-aware business workflows
   - No HTTP request/response objects

3. Servlet
   - HTTP handling
   - Session management
   - JSON input/output
   - Converts request DTOs to service calls and service results to API responses

Cross-cutting HTTP behavior is handled by servlet filters:

- exception-to-JSON handling
- request logging
- CORS
- no-store/security headers
- UTF-8 request/response encoding
- authentication
- CSRF validation
- request-scoped DB transaction handling

Layer boundary rules:

- servlet code may call services and write `ApiResponse` JSON
- service code may call DAOs and apply business rules
- DAO code must not know about sessions, servlets, HTTP status codes, or JSON
- shared HTTP concerns belong in filters or servlet support classes
- shared persistence concerns belong in `dao.common`

Current request lifecycle for `/api/*` requests:

1. `RequestLoggingFilter` measures the full request and logs the final status after inner filters finish.
2. `ExceptionHandlingFilter` catches unhandled exceptions and writes common JSON errors.
3. `CorsFilter` handles allowed origins and short-circuits preflight `OPTIONS`.
4. `SecurityHeadersFilter` adds no-store and browser security headers.
5. `CharacterEncodingFilter` sets UTF-8 request/response encoding.
6. `AuthFilter` validates session authentication except public auth endpoints.
7. `CsrfFilter` validates `X-CSRF-Token` for state-changing API requests.
8. `TransactionFilter` opens, commits, rolls back, and closes request-scoped DB transactions.

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

The common response envelope is mandatory for API endpoints:

```json
{
  "success": true,
  "data": {}
}
```

```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "Human readable message"
  }
}
```

Do not return raw DTOs or raw exception messages from API servlets.

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

Implemented auth response shape:

```json
{
  "success": true,
  "data": {
    "id": 1,
    "loginName": "tester",
    "email": "tester@example.com",
    "csrfToken": "..."
  }
}
```

Implemented auth error examples:

- `AUTH_REQUIRED`
- `INVALID_CREDENTIALS`
- `INVALID_REQUEST`
- `CSRF_INVALID`
- `INTERNAL_ERROR`

---

## Authentication

- Session-based
- Cookie handled by browser
- No JWT
- CSRF protection is active for state-changing `/api/*` requests
- Frontend must send `X-CSRF-Token` on `POST`, `PUT`, `PATCH`, and `DELETE` after login/session restore

Session details:

- session attribute for authenticated user: `user`
- stored object: `hu.laci.cms.backend.dto.auth.AuthenticatedUser`
- full persistence `User` is not stored in session
- successful login calls `request.changeSessionId()`
- session attribute for CSRF token: `csrfToken`
- CSRF token header: `X-CSRF-Token`

Public auth endpoints:

- `POST /api/auth/login`
- `POST /api/auth/logout` is public from the auth filter perspective, but CSRF-protected if a session exists and the request reaches the CSRF filter

CSRF rules:

- checked methods: `POST`, `PUT`, `PATCH`, `DELETE`
- skipped methods: `GET`, `HEAD`, `OPTIONS`
- skipped endpoint: `POST /api/auth/login`
- invalid or missing token returns `403 CSRF_INVALID`

Frontend requirements:

- call `GET /api/auth/me` on startup to restore session state
- store `data.csrfToken` from `login` and `me`
- include `credentials: "include"` for session-aware requests
- include `X-CSRF-Token` on every state-changing API request after login/session restore
- clear both user state and CSRF token on logout

---

## Database

- PostgreSQL
- Manual SQL (JDBC)
- Connection pool: HikariCP
- Request-scoped transactions through `TransactionFilter` and `TransactionContext`
- Prefer ANSI SQL where possible
- Use vendor-specific SQL only when justified
- Current connection entry point: `hu.laci.cms.backend.config.database.DatabaseConfig`

Connection configuration priority:

1. environment variables:
   - `DB_HOST`
   - `DB_PORT`
   - `DB_NAME`
   - `DB_USER`
   - `DB_PASSWORD`
2. `web.xml` context parameters
3. built-in defaults in `DatabaseConfig`

Important Docker note:

- inside Docker containers, `localhost` means the current container
- Dockerized backend/Jenkins builds must use `DB_HOST=postgres` on the compose network
- local non-Docker Tomcat normally needs `DB_HOST=localhost`

DAO layer notes:

- generic CRUD support lives in `hu.laci.cms.backend.dao.common.BaseDao`
- DAO instances are registered centrally through `hu.laci.cms.backend.dao.common.DaoRegistry`
- DAO IDs are `Long`
- generated `SELECT` SQL uses table-qualified columns with aliases, for example `users.id AS users_id`
- `findAll` supports multiple sort fields
- default sort is `id ASC`
- `LIKE` filters add `%` in the base DAO according to the requested `LikeFilterPosition`
- query filtering uses `QuerySpec` with entity property constants, not annotation-based filter classes
- supported query filter operations: `EQUALS`, `LIKE`, `LESS`, `LESS_OR_EQUALS`, `GREATER`, `GREATER_OR_EQUALS`, `IN`, `NOT_IN`, `BETWEEN`
- Java `Boolean`/`boolean` maps to database `VARCHAR(1)` values `T`/`F`
- DB mapping annotations live under `hu.laci.cms.backend.dao.common.annotations`

DAO responsibilities and behavior:

- `CrudDao<T, P>` defines common CRUD operations.
- `BaseDao<T, P>` implements reusable CRUD, query filtering, sorting, mapping, and SQL parameter handling.
- `BaseDao` keeps its subclass API intentionally narrow; internal SQL builders, parameter binding, and query assembly helpers are private implementation details.
- intended `BaseDao` subclass extension points are:
  - constructor with entity class
  - `getEntityClass()`
  - `getRowMapper()`
  - `findOneByProperty(...)`
  - `findCustomOne(...)`
  - `findCustomList(...)`
  - `executeCustomUpdate(...)`
  - `mapEntity(...)`
- Entity classes extend `BaseEntity`; `id` is always `Long`.
- `save(entity)` delegates to `create` when `id == null`, otherwise to `update`.
- `create` uses `INSERT ... RETURNING id`.
- `update` requires non-null id and fails if no row exists.
- `deleteById` returns whether a row was deleted.
- `findAll(querySpec)` defaults to `ORDER BY id ASC`.
- Query sort input is a list of `SortOrder<P>`, so multi-column order is supported.
- Query filters are built from `QuerySpec` criteria.
- Query joins are built from `JoinSpec`; joined entity mapping requires an explicit target property and is never inferred automatically.
- Join chains are supported when each joined entity is explicitly mapped before it is used as the owner of a nested join target.
- Joining the same entity type more than once requires explicit SQL aliases.
- Join `ON` clauses can include extra filter conditions in addition to the key equality.
- Join SQL aliases are supported, and join/filter/sort properties are validated before SQL execution.
- Entity metadata is annotation-driven through `DbTable` and `DbColumn`.
- Reflection metadata is cached per entity class.
- Result mapping uses generated column aliases, not raw column names.

Custom SQL support:

- `BaseDao` exposes protected custom SQL helpers for DAO subclasses:
  - `findCustomOne(operation, sql, parameters, rowMapper, errorMessage)`
  - `findCustomList(operation, sql, parameters, rowMapper, errorMessage)`
  - `executeCustomUpdate(operation, sql, parameters, errorMessage)`
- custom `SELECT` helpers can map arbitrary projection/DTO/entity result types through `RowMapper<R>`
- custom SQL helper Javadocs contain simple and more complex examples with non-empty parameter lists
- `executeCustomUpdate` is intended for `INSERT`, `UPDATE`, `DELETE`, and full-table `DELETE` style operations
- SQL operations still reuse `TransactionContext`, prepared statement parameter binding, boolean parameter conversion, SQL logging, and `DataAccessException` wrapping
- SQL with result rows, such as PostgreSQL `INSERT ... RETURNING`, should use a custom select helper or a dedicated helper rather than `executeCustomUpdate`

Static DAO convenience helpers:

- `BaseDao.saveEntity(entity)` resolves the matching DAO from `DaoRegistry` and calls its non-static `save`
- `BaseDao.loadEntity(entity)` requires a non-null id, loads the current DB row through the matching DAO, and copies DB values into the passed entity instance

Supported common type conversions:

- `Long` / `long`
- `Date`, `java.sql.Date`, `Timestamp`
- `Boolean` / `boolean` as DB `VARCHAR(1)` values `T` and `F`
- `BaseEntity` references as foreign-key id values

Specialized future conversions, such as JSON columns or richer audit support, should be added deliberately when a concrete model needs them.

Audit model direction:

- audit fields should be introduced through a dedicated model base class, for example an `AuditableEntity` that extends `BaseEntity`
- when this is added, `DbColumn` may need insert/update flags, for example for `created_at` and `updated_at`

Transaction behavior:

- `TransactionFilter` starts one DB transaction per request.
- successful request processing commits the transaction.
- exceptions trigger rollback.
- `TransactionContext.setRollbackOnly()` can be called explicitly when code catches an error but still returns a handled response.
- if rollback-only is set, request end performs rollback even if the servlet/filter chain returns normally.
- `TransactionContext.openConnection()` returns the request-bound connection when one exists.
- DAO code should use `TransactionContext.openConnection()` rather than directly opening raw connections.

Logging:

- SQL operations are logged from `BaseDao`.
- mutation operations log entity type and id where applicable.
- sensitive parameter names such as password/hash/token/secret are masked in SQL parameter logs.

---

## Current Structure

```text
cms-app/
|-- src/
|   |-- main/
|   |   |-- java/
|   |   |   `-- hu/laci/cms/
|   |   |       |-- Main.java
|   |   |       `-- backend/
|   |   |           |-- config/
|   |   |           |   |-- app/
|   |   |           |   `-- database/
|   |   |           |-- dao/
|   |   |           |   |-- common/
|   |   |           |   |   `-- annotations/
|   |   |           |   `-- user/
|   |   |           |-- dto/
|   |   |           |   |-- auth/
|   |   |           |   `-- common/
|   |   |           |-- model/
|   |   |           |   |-- common/
|   |   |           |   |   `-- annotations/
|   |   |           |   `-- user/
|   |   |           |-- service/
|   |   |           `-- servlet/
|   |   |               |-- auth/
|   |   |               |-- filter/
|   |   |               |-- health/
|   |   |               `-- support/
|   |   |-- resources/
|   |   `-- webapp/
|   |       `-- WEB-INF/
|   |           `-- web.xml
|   `-- test/
|       |-- java/
|       `-- resources/
|-- docker/
|   |-- jenkins/
|   |-- postgres/
|   `-- tomcat/
|-- skills/
|-- FRONTEND_BOOTSTRAP_PLAN.md
|-- FRONTEND_HANDOFF.md
|-- SESSION_CONTEXT.md
|-- docker-compose.yml
|-- Jenkinsfile
|-- pom.xml
|-- project.md
`-- agent.md
```

---

## Target Backend Structure

- `hu.laci.cms.backend.model`
- `hu.laci.cms.backend.dao`
- `hu.laci.cms.backend.service`
- `hu.laci.cms.backend.servlet`
- `hu.laci.cms.backend.dto`
- `hu.laci.cms.backend.config`

Package conventions:

- `model.*`: persistence/domain-like model classes and query property definitions
- `model.common`: common model, query, filter operation, and sort-order support
- `dao.*`: DAO interfaces and implementations
- `dao.common`: shared DAO infrastructure
- `dao.common.annotations`: DB mapping annotations
- `dto.*`: API-facing data transfer objects
- `service.*`: business logic and validation
- `servlet.auth`: authentication endpoints
- `servlet.filter`: cross-cutting HTTP filters
- `servlet.support`: reusable servlet helpers
- `config.database`: DB pool and transaction context
- `config.app`: app initialization listeners

---

## Development Principles

- No frameworks (no Spring)
- No ORM
- Keep code simple and explicit
- Respect layer boundaries
- Avoid unnecessary abstractions
- Prefer existing project patterns over new local styles
- Add abstractions only when they reduce real duplication or clarify shared behavior
- Keep generated SQL readable and loggable
- Use prepared statements for SQL parameters
- Keep API errors in the common response envelope
- Avoid leaking implementation details or stack traces to frontend responses
- Treat DB-backed DAO tests as integration tests, even if they run under Maven Surefire
- Write Javadoc when creating new classes and public/protected APIs, not as a separate final cleanup.
- Every class should have class-level Javadoc.
- Public/protected APIs should document parameters, return values, relevant exceptions, and usage examples when helpful.
- Private methods need Javadoc only for non-trivial logic or important invariants.
- DAO/query/transaction/infrastructure APIs should have more detailed Javadoc than DTO getters or standard servlet/filter/listener overrides.

---

## Current Backend Status

- `User`, `UserDao`, `UserDaoImpl`, `AuthService`, `AuthServiceException`, `DatabaseConfig` already exist
- generic DAO CRUD, `QuerySpec` filtering/sorting/join support, and custom SQL helpers are implemented in `BaseDao`
- DAO integration tests currently cover user DAO behavior, CRUD, transaction commit/rollback, boolean mapping, filtering, sorting, relational filters, `IN` / `NOT_IN` / `BETWEEN`, joins, duplicate join alias validation, repeated joined entity aliases, nested join mapping, extra join conditions, static DAO convenience helpers, and custom SQL helpers
- application startup/shutdown listeners initialize and close shared infrastructure:
  - `hu.laci.cms.backend.config.database.DatabaseConfigListener`
  - `hu.laci.cms.backend.config.app.DaoRegistryListener`
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

1. `requestLoggingFilter`
2. `exceptionHandlingFilter`
3. `corsFilter`
4. `securityHeadersFilter`
5. `characterEncodingFilter`
6. `authFilter`
7. `csrfFilter`
8. `transactionFilter`

Current auth endpoints:

### `POST /api/auth/login`

Request:

```json
{
  "loginName": "tester",
  "password": "pw"
}
```

Success:

```json
{
  "success": true,
  "data": {
    "id": 1,
    "loginName": "tester",
    "email": "tester@example.com",
    "csrfToken": "..."
  }
}
```

### `GET /api/auth/me`

Returns the same authenticated user shape as login, including `csrfToken`, when the session is valid.

### `POST /api/auth/logout`

Requires `X-CSRF-Token` after login/session restore.

Success:

```json
{
  "success": true,
  "data": {
    "message": "Logged out"
  }
}
```

Current filter responsibilities:

| Filter | Scope | Responsibility |
| --- | --- | --- |
| `RequestLoggingFilter` | `/*` | Logs final request method, target, status, duration, remote address, and user. |
| `ExceptionHandlingFilter` | `/*` | Converts unhandled exceptions to common JSON error responses when possible. |
| `CorsFilter` | `/*` | Handles allowed origins and `OPTIONS` preflight. |
| `SecurityHeadersFilter` | `/*` | Adds no-store cache and browser hardening headers. |
| `CharacterEncodingFilter` | `/*` | Sets UTF-8 request and response encoding. |
| `AuthFilter` | `/api/*` | Requires authenticated session except public auth paths. |
| `CsrfFilter` | `/api/*` | Requires CSRF token for state-changing API requests. |
| `TransactionFilter` | `/*` | Wraps request processing in DB transaction scope. |

Security headers currently set:

- `Cache-Control: no-store`
- `Pragma: no-cache`
- `Expires: 0`
- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `Referrer-Policy: no-referrer`
- `Permissions-Policy: geolocation=(), microphone=(), camera=()`

`Strict-Transport-Security` is intentionally not enabled yet because HTTPS is not currently configured.

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

Docker Compose services:

| Service | Container | Purpose |
| --- | --- | --- |
| `postgres` | `cms-postgres` | PostgreSQL 15 database with `cms_db`. |
| `tomcat` | `cms-tomcat` | Runs the packaged WAR on Tomcat. |
| `jenkins` | `cms-jenkins` | CI build/deploy runner. |

Important ports:

- PostgreSQL host port: `5433` mapped to container `5432`
- Tomcat host port: `8081` mapped to container `8080`
- Jenkins host port: `8082` mapped to container `8080`

Typical Docker commands:

```powershell
docker compose up -d --build
docker compose ps
docker compose down
```

Do not use `docker compose down -v` unless the PostgreSQL and Jenkins volumes should be deleted.

Jenkins note:

- the Maven build runs DAO tests during `mvn package`
- those tests require PostgreSQL
- Jenkins therefore needs DB environment variables and network access to `postgres:5432`

---

## Working Notes

- `SESSION_CONTEXT.md` stores the latest implementation summary and local runtime state for follow-up sessions
- `FRONTEND_HANDOFF.md` stores the backend contract for the separate React frontend repo
- `FRONTEND_BOOTSTRAP_PLAN.md` stores the recommended frontend repo bootstrap plan
- keep `project.md` focused on stable project context and intended architecture
- keep machine-specific or temporary setup details out of this file unless they become permanent project conventions

Runtime URLs:

- local standalone Tomcat app context: `http://localhost:8081/cms-app`
- Docker Tomcat root context: `http://localhost:8081`
- health endpoint when deployed under `/cms-app`: `http://localhost:8081/cms-app/hello`
- health endpoint in Docker root context: `http://localhost:8081/hello`

Frontend integration notes:

- Vite dev proxy can map `/api` to `http://localhost:8081/cms-app/api`
- backend CORS currently allows `http://localhost:5173` and `http://127.0.0.1:5173`
- even with CORS, same-origin/reverse-proxy deployment remains simpler for session auth
- frontend API client should centralize `credentials: "include"` and CSRF header handling

Testing notes:

- `mvn test` runs DB-backed DAO tests
- `mvn package` also runs tests unless skipped
- current verified test count: 45
- PostgreSQL schema comes from `docker/postgres/init.sql`
- tests clean up their own `dao_test_` user data and temporary boolean test table

---

## Workflow

- Work in small steps
- Implement feature parts separately:
  - model -> DAO -> service -> servlet
- Avoid large one-step implementations
- For API changes, update `FRONTEND_HANDOFF.md`.
- For frontend bootstrap guidance changes, update `FRONTEND_BOOTSTRAP_PLAN.md`.
- For session-to-session continuity, update `SESSION_CONTEXT.md`.
- For durable architecture/project conventions, update this file.
