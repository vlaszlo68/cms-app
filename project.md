# CMS Project Context

## Overview

Custom-built Content Management System (CMS) with a Java backend and React frontend.

Goal: clean, minimal, framework-light architecture with full control over implementation.

The backend is intentionally servlet/JDBC based. The project avoids Spring, ORM frameworks, and large infrastructure abstractions so the request lifecycle, transaction boundaries, SQL generation, and HTTP contract remain explicit and easy to inspect.

The frontend lives in a separate React repository. Backend API behavior is documented in this file and in the source code; old frontend handoff planning files were removed after being copied to the frontend repository.

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
- HTTP session context propagation
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
7. `AppSessionContextFilter` copies selected application session data into request-local `SessionContext`.
8. `CsrfFilter` validates `X-CSRF-Token` for state-changing API requests.
9. `TransactionFilter` opens, commits, rolls back, and closes request-scoped DB transactions.

---

## API

- REST + JSON
- Base path: `/api`
- Common API response envelope:
  - success: `{ "success": true, "data": ... }`
  - error: `{ "success": false, "error": { "code": "...", "message": "..." } }`
  - validation failures may include `error.validationErrors`

Examples:

- `POST /api/auth/login`
- `GET /api/auth/config`
- `GET /api/auth/captcha`
- `POST /api/auth/register`
- `POST /api/auth/logout`
- `GET /api/auth/me`
- `POST /api/users/{id}/approve`
- `POST /api/users/{id}/reject`
- `GET /api/pages`
- `POST /api/pages`
- `PUT /api/pages/{id}`
- `DELETE /api/pages/{id}`
- `GET /api/media`
- `GET /api/media/{id}`
- `GET /api/media/{id}/content`
- `POST /api/media`
- `DELETE /api/media/{id}`

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
  - `GET /api/auth/config`
  - `GET /api/auth/captcha`
  - `POST /api/auth/register`
  - `POST /api/auth/logout`
  - `GET /api/auth/me`
- auth endpoints now use the common API response envelope
- successful auth responses include a CSRF token in `data.csrfToken`
- local standalone Tomcat deploy path in current development is:
  - `http://localhost:8080/cms-app/api/auth/login`
  - `http://localhost:8080/cms-app/api/auth/config`
  - `http://localhost:8080/cms-app/api/auth/captcha`
  - `http://localhost:8080/cms-app/api/auth/register`
  - `http://localhost:8080/cms-app/api/auth/logout`
  - `http://localhost:8080/cms-app/api/auth/me`
- Docker Tomcat deploy path in current development is:
  - `http://localhost:8081/api/auth/login`
  - `http://localhost:8081/api/auth/config`
  - `http://localhost:8081/api/auth/captcha`
  - `http://localhost:8081/api/auth/register`
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
    "role": "ADMIN",
    "csrfToken": "..."
  }
}
```

Implemented auth error examples:

- `AUTH_REQUIRED`
- `INVALID_CREDENTIALS`
- `INVALID_REQUEST`
- `CAPTCHA_INVALID`
- `RATE_LIMITED`
- `DUPLICATE_LOGIN_NAME`
- `DUPLICATE_EMAIL_ADDRESS`
- `VALIDATION_ERROR`
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

- servlet/filter code accesses session state through `hu.laci.cms.backend.config.session.AppSessionManager`
- the backing store is selected by `session.store.mode`
- supported store modes:
  - `http`: current Tomcat `HttpSession` backed behavior
  - `jdbc`: PostgreSQL-backed application session store
  - `redis`: planned future store mode, not implemented yet
- configuration priority:
  1. `SESSION_STORE_MODE` environment variable
  2. `web.xml` `session.store.mode` context parameter
  3. built-in fallback `http`
- explicit invalid store mode values fail startup/configuration instead of silently falling back
- `web.xml` explicitly defaults to `session.store.mode=http`
- swarm test configuration sets `SESSION_STORE_MODE=jdbc`
- authenticated user snapshot: `hu.laci.cms.backend.dto.auth.AuthenticatedUser`
- full persistence `User` is not stored in session
- `AuthenticatedUser` currently contains `id`, `loginName`, `email`, and `role`
- successful login creates a fresh authenticated application session
- in `http` mode the current Tomcat `HttpSession` behavior is preserved
- in `jdbc` mode the browser receives `CMS_SESSION_ID` and state is stored in PostgreSQL
- CSRF token is stored in the application session and returned to the frontend from login and `/api/auth/me`
- CSRF token header: `X-CSRF-Token`

JDBC session store schema:

- `app_sessions`
  - stores session lifecycle, authenticated user snapshot, CSRF token, and invalidation state
  - primary key is `id_hash`, a SHA-256 hash of the browser session id
- `app_session_attributes`
  - stores structured session attributes as typed JSON payloads
  - references `app_sessions(id_hash)` through `session_id_hash`
  - uses `ON DELETE CASCADE`
  - unique key: `(session_id_hash, attribute_name)`
- current structured attribute:
  - `attribute_name = captcha`
  - `attribute_type = CAPTCHA_STATE`

Redis design note:

- Redis is not implemented yet.
- The current session abstraction, cookie handling, session id generation, and typed attribute model are intentionally store-neutral so `RedisAppSessionStore` can be added later without servlet/filter/auth rewrites.
- Long-term cluster direction is external session state (`jdbc` first, Redis later), not Nginx sticky sessions.

Public auth endpoints:

- `POST /api/auth/login`
- `GET /api/auth/config`
- `GET /api/auth/captcha`
- `POST /api/auth/register`
- `POST /api/auth/logout` is public from the auth filter perspective, but CSRF-protected if a session exists and the request reaches the CSRF filter

CSRF rules:

- checked methods: `POST`, `PUT`, `PATCH`, `DELETE`
- skipped methods: `GET`, `HEAD`, `OPTIONS`
- skipped endpoints:
  - `POST /api/auth/login`
  - `POST /api/auth/register`
- invalid or missing token returns `403 CSRF_INVALID`

Registration and login hardening:

- `GET /api/auth/captcha?purpose=login|registration` returns SVG content and exposes the generated id in the `X-Captcha-Id` response header.
- `GET /api/auth/config` exposes whether CAPTCHA is enabled for login and registration screens.
- `POST /api/auth/register` creates `USER`, `active=false`, `registrationStatus=PENDING` accounts.
- login accepts `captchaId` and `captchaAnswer` when `captcha.login.enabled=true`.
- registration requires `captchaId` and `captchaAnswer`; CAPTCHA values are stored as `CAPTCHA_STATE` in the active application session with purpose, created time, and attempt count.
- login and registration use `AppSessionManager.validateCaptcha(...)` for session-backed CAPTCHA lookup, validation result persistence, and challenge cleanup; `CaptchaService` still owns the domain validation rules.
- CAPTCHA validation enforces 3-minute TTL, 1-second minimum solve time, and 2 attempts per challenge.
- CAPTCHA generation is rate limited by `request.getRemoteAddr() + sessionId`.
- CAPTCHA is purpose-bound: a login challenge cannot be used for registration, and a registration challenge cannot be used for login.
- login and registration reject non-empty `captchaHoneypot` values.
- login returns only `INVALID_CREDENTIALS` for missing users, bad passwords, inactive accounts, and temporary lockouts.
- login rate limiting is in-memory and keyed by `loginName + request.getRemoteAddr()`.
- registration rate limiting is in-memory and keyed by `request.getRemoteAddr()`.
- password policy is configurable through `web.xml` context parameters and enforced for registration and password changes.
- CAPTCHA display and enforcement is configurable through `captcha.login.enabled` and `captcha.registration.enabled`; both default to `true`.
- password policy errors are returned as structured `error.validationErrors` codes such as `TOO_SHORT`, `MISSING_UPPERCASE`, `MISSING_DIGIT`, and `MISSING_SPECIAL`.

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
- Application startup runs versioned SQL migrations through `DatabaseMigrationRunner`
- Applied migration versions, script names, and checksums are tracked in `schema_migrations`

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
- Java enum values map to database `VARCHAR` values through `Enum.name()`
- DB mapping annotations live under `hu.laci.cms.backend.dao.common.annotations`
- `DbColumn` supports `insertable` and `updatable` flags for generated CRUD SQL

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
- Audit-capable entity classes extend `AuditableEntity`, which adds `createdAt`, `updatedAt`, `createdBy`, and `updatedBy`.
- `save(entity)` delegates to `create` when `id == null`, otherwise to `update`.
- `create` uses `INSERT ... RETURNING id`.
- `create` automatically fills audit fields for `AuditableEntity` instances.
- `update` requires non-null id and fails if no row exists.
- `update` automatically refreshes `updatedAt` and `updatedBy` for `AuditableEntity` instances.
- `deleteById` returns whether a row was deleted.
- `delete(entity)` requires a non-null entity and id, then delegates to `deleteById(entity.getId())`.
- `findAll(querySpec)` defaults to `ORDER BY id ASC`.
- Query sort input is a list of `SortOrder<P>`, so multi-column order is supported.
- Query filters are built from `QuerySpec` criteria.
- Query joins are built from `JoinSpec`; joined entity mapping requires an explicit target property and is never inferred automatically.
- Join-aware select SQL is assembled with both joined table columns and the corresponding SQL `JOIN` clauses in the same select builder path.
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
- `BaseDao.deleteEntity(entity)` requires a non-null id, resolves the matching DAO from `DaoRegistry`, and calls its non-static `delete`
- `BaseDao.loadEntity(entity)` requires a non-null id, loads the current DB row through the matching DAO, and copies DB values into the passed entity instance

Supported common type conversions:

- `Long` / `long`
- `Date`, `java.sql.Date`, `Timestamp`
- `Boolean` / `boolean` as DB `VARCHAR(1)` values `T` and `F`
- `enum` values as DB `VARCHAR` values using the enum constant name
- `BaseEntity` references as foreign-key id values

Specialized future conversions, such as JSON columns, should be added deliberately when a concrete model needs them.

Session-derived request context:

- `AppSessionContextFilter` populates `SessionContext` for the current request.
- `SessionContext` is a thread-local holder for data derived from the active application session.
- It currently exposes the current authenticated user id for audit field population.
- The context is cleared at the end of every request.

Audit behavior:

- `AuditableEntity` adds `createdAt`, `updatedAt`, `createdBy`, and `updatedBy`.
- `AuditableProperty` provides common property descriptors for audit fields; auditable entity-specific property classes can extend it.
- `createdAt` and `createdBy` are insert-only in generated DAO SQL.
- `updatedAt` and `updatedBy` are refreshed by `BaseDao.update`.
- `createdBy` and `updatedBy` are nullable; no foreign key is currently defined.

Migration behavior:

- migration files live under `src/main/resources/db/migration/`
- file names use the pattern `V<version>__<description>.sql`
- startup creates `schema_migrations` if needed
- migrations are applied in version order
- already applied migrations are skipped after script name and checksum validation
- PostgreSQL advisory locking prevents concurrent migration execution by multiple app instances
- `docker/postgres/init.sql` no longer owns the schema; it only points to app-managed migrations

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
|   |   |           |   |-- database/
|   |   |           |   |-- security/
|   |   |           |   `-- session/
|   |   |           |-- dao/
|   |   |           |   |-- common/
|   |   |           |   |   `-- annotations/
|   |   |           |   |-- media/
|   |   |           |   |-- menu/
|   |   |           |   |-- page/
|   |   |           |   `-- user/
|   |   |           |-- dto/
|   |   |           |   |-- auth/
|   |   |           |   |-- common/
|   |   |           |   |-- media/
|   |   |           |   |-- menu/
|   |   |           |   |-- page/
|   |   |           |   `-- user/
|   |   |           |-- model/
|   |   |           |   |-- common/
|   |   |           |   |-- media/
|   |   |           |   |-- menu/
|   |   |           |   |-- page/
|   |   |           |   `-- user/
|   |   |           |-- service/
|   |   |           |   |-- auth/
|   |   |           |   |-- media/
|   |   |           |   |-- menu/
|   |   |           |   |-- page/
|   |   |           |   |-- security/
|   |   |           |   `-- user/
|   |   |           `-- servlet/
|   |   |               |-- auth/
|   |   |               |-- filter/
|   |   |               |-- health/
|   |   |               |-- media/
|   |   |               |-- menu/
|   |   |               |-- page/
|   |   |               `-- support/
|   |   |-- resources/
|   |   |   `-- db/
|   |   |       `-- migration/
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
|-- AGENTS.md
|-- cluster-jdbc-todo.md
|-- docker-compose.yml
|-- Jenkinsfile
|-- pom.xml
`-- project.md
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
- `config.security`: centralized auth and password policy configuration
- `config.session`: request-local context populated from HTTP session data
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
- Avoid `var`; use explicit Java types in production and test code
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
- Page, Media, Menu, MenuItem, PageType, and PageBlock modules are implemented through the model, DAO, service, DTO, and servlet layers.
- Template and Site Settings modules are implemented. Templates describe what the frontend renders; pages hold content; site settings hold global website data.
- Menu items support `PAGE` and `URL` targets. PAGE targets persist `pageId` and clear `targetUrl`; URL targets persist `targetUrl` and clear `pageId`.
- The public menu tree API is independent from Page, Media, and Template implementations.
- Startup migrations ensure the `MAIN` and `FOOTER` menus exist.
- `User` currently has `role`, `active`, and `registrationState` fields in addition to identity and credential fields
- user enum models currently include `UserRole` and `RegistrationState`
- generic DAO CRUD, `QuerySpec` filtering/sorting/join support, and custom SQL helpers are implemented in `BaseDao`
- DAO integration tests currently cover user DAO behavior, CRUD, transaction commit/rollback, boolean mapping, filtering, sorting, relational filters, `IN` / `NOT_IN` / `BETWEEN`, joins, joined-entity filtering, duplicate join alias validation, repeated joined entity aliases, nested join mapping, extra join conditions, static DAO convenience helpers, and custom SQL helpers
- application startup/shutdown listeners initialize and close shared infrastructure:
  - `hu.laci.cms.backend.config.database.DatabaseConfigListener`
  - `hu.laci.cms.backend.config.app.DaoRegistryListener`
- auth servlet layer is now implemented with:
  - `hu.laci.cms.backend.servlet.auth.AuthServlet`
  - `hu.laci.cms.backend.servlet.auth.AuthConfigServlet`
  - `hu.laci.cms.backend.servlet.auth.CaptchaServlet`
  - `hu.laci.cms.backend.servlet.auth.RegisterServlet`
  - `hu.laci.cms.backend.servlet.auth.LogoutServlet`
  - `hu.laci.cms.backend.servlet.auth.MeServlet`
  - `hu.laci.cms.backend.dto.auth.AuthConfigResponse`
  - `hu.laci.cms.backend.dto.auth.LoginRequest`
  - `hu.laci.cms.backend.dto.auth.RegisterRequest`
  - `hu.laci.cms.backend.dto.auth.AuthenticatedUser`
  - `hu.laci.cms.backend.dto.common.ApiResponse`
  - `hu.laci.cms.backend.dto.common.ApiErrorResponse`
  - `hu.laci.cms.backend.servlet.support.JsonServletSupport`
  - `hu.laci.cms.backend.servlet.filter.AuthFilter`
  - `hu.laci.cms.backend.servlet.filter.AppSessionContextFilter`
  - `hu.laci.cms.backend.servlet.filter.ExceptionHandlingFilter`
  - `hu.laci.cms.backend.servlet.filter.RequestLoggingFilter`
  - `hu.laci.cms.backend.servlet.filter.CorsFilter`
  - `hu.laci.cms.backend.servlet.filter.SecurityHeadersFilter`
  - `hu.laci.cms.backend.servlet.filter.CharacterEncodingFilter`
  - `hu.laci.cms.backend.servlet.filter.CsrfFilter`
  - `hu.laci.cms.backend.servlet.health.HelloServlet`
  - `hu.laci.cms.backend.servlet.support.CsrfTokenSupport`
  - `hu.laci.cms.backend.config.session.AppSession`
  - `hu.laci.cms.backend.config.session.AppSessionAttribute`
  - `hu.laci.cms.backend.config.session.AppSessionAttributeType`
  - `hu.laci.cms.backend.config.session.AppSessionConfig`
  - `hu.laci.cms.backend.config.session.AppSessionConfigListener`
  - `hu.laci.cms.backend.config.session.AppSessionManager`
  - `hu.laci.cms.backend.config.session.AppSessionStore`
  - `hu.laci.cms.backend.config.session.AppSessionStoreMode`
  - `hu.laci.cms.backend.config.session.HttpSessionAppSessionStore`
  - `hu.laci.cms.backend.config.session.JdbcAppSessionStore`
  - `hu.laci.cms.backend.config.session.SessionCookieSupport`
  - `hu.laci.cms.backend.config.session.SessionIdGenerator`
- JSON request/response handling currently uses Gson
- successful JSON API responses are wrapped as `success/data`
- JSON API errors are wrapped as `success/error.code/error.message`
- validation errors can include `success/error.validationErrors`
- session-based authentication is active through `AppSessionManager`
- session-backed CAPTCHA validation state is read and updated through `AppSessionManager`
- `http` mode preserves the Tomcat `HttpSession` backed behavior
- `jdbc` mode stores session state in PostgreSQL tables created by `V5__app_sessions.sql`
- successful login creates a fresh authenticated application session
- the session stores an `AuthenticatedUser` snapshot, not the full persistence `User`
- `AuthenticatedUser` and auth responses include the user `role`
- successful login creates a session CSRF token through the application session abstraction
- `POST /api/auth/login` and `GET /api/auth/me` return `csrfToken`
- `AuthFilter` uses `request.getServletPath()`, so public auth endpoints work both under root context and `/cms-app`
- old frontend handoff and bootstrap planning documents were copied to the separate frontend repo and removed from this backend repo

Current filter order in `web.xml`:

1. `requestLoggingFilter`
2. `exceptionHandlingFilter`
3. `corsFilter`
4. `securityHeadersFilter`
5. `characterEncodingFilter`
6. `authFilter`
7. `appSessionContextFilter`
8. `csrfFilter`
9. `transactionFilter`

Current auth endpoints:

### `POST /api/auth/login`

Request:

```json
{
  "loginName": "tester",
  "password": "pw",
  "captchaId": "captcha-id-from-header",
  "captchaAnswer": "10",
  "captchaHoneypot": ""
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
    "role": "ADMIN",
    "csrfToken": "..."
  }
}
```

### `GET /api/auth/me`

Returns the same authenticated user shape as login, including `csrfToken`, when the session is valid.

### `GET /api/auth/config`

Public endpoint for frontend auth screen feature flags.

```json
{
  "success": true,
  "data": {
    "loginCaptchaEnabled": true,
    "registrationCaptchaEnabled": true,
    "passwordPolicy": {
      "minLength": 2,
      "requireUppercase": false,
      "requireLowercase": false,
      "requireDigit": false,
      "requireSpecial": false
    }
  }
}
```

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

### `GET /api/auth/captcha`

Returns an SVG CAPTCHA challenge for public login or registration.

- response content type: `image/svg+xml`
- response header: `X-Captcha-Id`
- query parameter: `purpose=login` or `purpose=registration`; missing or invalid values default to `registration`
- request must use credentials so the session-backed answer can be validated later
- challenge validity: 3 minutes, 2 validation attempts, minimum 1 second solve time

### `POST /api/auth/register`

Public, CSRF-exempt registration endpoint.

Request:

```json
{
  "loginName": "newuser",
  "userName": "New User",
  "emailAddress": "newuser@example.com",
  "password": "Password123!",
  "captchaId": "captcha-id-from-header",
  "captchaAnswer": "10",
  "captchaHoneypot": ""
}
```

Success creates a pending inactive user:

```json
{
  "success": true,
  "data": {
    "id": 123,
    "loginName": "newuser",
    "userName": "New User",
    "emailAddress": "newuser@example.com",
    "role": "USER",
    "active": false,
    "registrationStatus": "PENDING",
    "createdAt": "...",
    "updatedAt": "..."
  }
}
```

### `POST /api/users/{id}/approve`

Admin-only endpoint. Requires CSRF. Sets `registrationStatus=COMPLETED` and `active=true`.

### `POST /api/users/{id}/reject`

Admin-only endpoint. Requires CSRF. Sets `registrationStatus=REJECTED` and `active=false`.

### Media endpoints

Admin-only media library endpoints live under `/api/media`.

- `GET /api/media`: returns media metadata list. By default only active records are returned; `activeOnly=false` includes inactive records.
- `GET /api/media/{id}`: returns one media metadata record as JSON.
- `GET /api/media/{id}/content`: returns the stored media content as a binary response, without the common JSON envelope. The response uses the stored MIME type as `Content-Type`, sets `Content-Length`, and uses `Content-Disposition: inline` so browsers can preview supported file types.
- `POST /api/media`: accepts multipart form upload with required `file` part and optional `description`.
- `DELETE /api/media/{id}`: hard-deletes the media storage content and metadata row.

The current local storage mode is configured through `media.storage.type`. `DATABASE` stores binary content in `media_contents`; `FILESYSTEM` stores files under `media.filesystem.path`. `MINIO` and `S3` are declared storage types but are not implemented yet.

### Menu endpoints

Administrator menu CRUD endpoints require an authenticated `ADMIN` session. State-changing requests also require the normal CSRF header.

- `GET /api/menus`: lists menus.
- `GET /api/menus/{id}`: returns one menu.
- `POST /api/menus`: creates a menu.
- `PUT /api/menus/{id}`: updates a menu.
- `DELETE /api/menus/{id}`: deletes a menu and its items.
- `GET /api/menus/{id}/items`: returns the flat, ordered item list.
- `POST /api/menu-items`: creates a menu item.
- `PUT /api/menu-items/{id}`: updates a menu item.
- `DELETE /api/menu-items/{id}`: deletes a menu item and its child subtree.

Target normalization:

- `PAGE`: requires `pageId`; `targetUrl` is stored as `null`.
- `URL`: requires a non-blank `targetUrl`; `pageId` is stored as `null`.
- missing `targetType` is interpreted as `PAGE` for backward compatibility.

`GET /api/public/menus/{code}` is authentication-free and returns the active menu as a visible, ordered tree. Public items contain `title`, `targetType`, `pageId`, `targetUrl`, and `children`.

`V9__menus.sql` creates the menu tables. `V10__menu_item_targets_and_default_menus.sql` adds target support and idempotently creates `MAIN` and `FOOTER`.

Current local `FOOTER` content:

- `ÁSZF` -> PAGE with slug `aszf`
- `GDPR` -> PAGE with slug `gdpr`
- `Support` -> URL `mailto:support@example.com`

### Template endpoints

Template records are configuration only; they do not store HTML, React code, or other executable rendering logic.

- `GET /api/templates`: lists templates.
- `GET /api/templates/{id}`: returns one template.
- `POST /api/templates`: creates a template.
- `PUT /api/templates/{id}`: updates a template.
- `DELETE /api/templates/{id}`: deactivates a template.

All template endpoints require `ADMIN`. Template codes are unique. `STANDARD`, `LANDING`, and `BLOG` are created by migration.

Pages expose an optional `templateId` request field and include it in list/detail responses. When omitted during create or update, the service persists the `STANDARD` template id.

### Site Settings endpoints

Site Settings stores one global configuration record, independently from page content and template selection.

- `GET /api/site-settings`: returns the global settings.
- `PUT /api/site-settings`: replaces the global settings fields.

Both endpoints require `ADMIN`. The database enforces a single `site_settings` row.

`V11__templates_and_site_settings.sql` creates templates, adds `pages.template_id`, backfills existing pages with `STANDARD`, and creates the singleton site settings record.

### Page types and blocks

Pages support two content models:

- `CONTENT`: traditional CMS page; non-blank `content` is required.
- `BLOCK`: composite page; `content` is optional and the page is assembled from ordered `PageBlock` records.

Page create, update, list, and detail DTOs contain `pageType`. Missing `pageType` remains backward compatible and is interpreted as `CONTENT`.

Page block admin endpoints:

- `GET /api/pages/{id}/blocks`: returns all blocks ordered by `sortOrder`, then id.
- `GET /api/page-blocks/{id}`: returns one block.
- `POST /api/page-blocks`: creates a block.
- `PUT /api/page-blocks/{id}`: updates a block.
- `DELETE /api/page-blocks/{id}`: deletes a block.

`GET /api/pages/{id}?includeBlocks=true` returns `{ "page": ..., "blocks": [...] }`.

`blockType` is currently free text. `configJson` is stored as opaque text and is intentionally not parsed or validated by the backend. Deleting a page cascades to its blocks.

`V12__page_types_and_blocks.sql` adds `pages.page_type`, permits null page content for BLOCK pages, and creates `page_blocks`.

Current filter responsibilities:

| Filter | Scope | Responsibility |
| --- | --- | --- |
| `RequestLoggingFilter` | `/*` | Logs final request method, target, status, duration, remote address, and user. |
| `ExceptionHandlingFilter` | `/*` | Converts unhandled exceptions to common JSON error responses when possible. |
| `CorsFilter` | `/*` | Handles allowed origins and `OPTIONS` preflight. |
| `SecurityHeadersFilter` | `/*` | Adds no-store cache and browser hardening headers. |
| `CharacterEncodingFilter` | `/*` | Sets UTF-8 request and response encoding. |
| `AuthFilter` | `/api/*` | Requires authenticated session except public auth paths and `/api/public/*`. |
| `AppSessionContextFilter` | `/*` | Copies selected application session data into request-local `SessionContext`. |
| `CsrfFilter` | `/api/*` | Requires CSRF token for state-changing API requests. |
| `TransactionFilter` | `/*` | Wraps request processing in DB transaction scope. |

Session store configuration:

| Setting | Source priority | Default | Notes |
| --- | --- | --- | --- |
| `session.store.mode` / `SESSION_STORE_MODE` | env, then `web.xml`, then built-in | `http` | `http` and `jdbc` are implemented; `redis` is planned. |
| `session.cookie.name` / `SESSION_COOKIE_NAME` | env, then `web.xml`, then built-in | `CMS_SESSION_ID` | Used by external stores such as `jdbc`; `http` mode still uses Tomcat session handling. |
| `session.timeout.minutes` / `SESSION_TIMEOUT_MINUTES` | env, then `web.xml`, then built-in | `30` | Used for external session expiry. |
| `session.cookie.secure` / `SESSION_COOKIE_SECURE` | env, then `web.xml`, then built-in | `false` | Should be `true` under HTTPS production deployments. |
| `session.cookie.sameSite` / `SESSION_COOKIE_SAMESITE` | env, then `web.xml`, then built-in | `Lax` | Suitable for same-origin/reverse-proxy deployment. |

Rate limiter store configuration:

| Setting | Source priority | Default | Notes |
| --- | --- | --- | --- |
| `rateLimiter.store.mode` / `RATE_LIMITER_STORE_MODE` | env, then `web.xml`, then built-in | `memory` | `memory` and `jdbc` are implemented; `redis` is planned. |

Rate limiter details:

- login failed-attempt limiter uses namespace `login_failed_attempts`
- registration attempt limiter uses namespace `registration_attempts`
- CAPTCHA generation limiter uses namespace `captcha_generation`
- `memory` mode preserves the previous process-local behavior
- `jdbc` mode stores shared limiter state in the `rate_limits` table created by `V6__rate_limits.sql`
- swarm test configuration sets `RATE_LIMITER_STORE_MODE=jdbc`
- Redis remains the planned long-term store for short-lived limiter counters, but is not implemented yet

Security headers currently set:

- `Cache-Control: no-store`
- `Pragma: no-cache`
- `Expires: 0`
- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `Referrer-Policy: no-referrer`
- `Permissions-Policy: geolocation=(), microphone=(), camera=()`

`Strict-Transport-Security` is intentionally not enabled yet because HTTPS is not currently configured.

Exposed CORS response headers:

- `X-Captcha-Id`

---

## Current DevOps Status

- `docker-compose.yml` exists in the project root
- `docker/tomcat/Dockerfile` exists for WAR-based Tomcat image build
- current compose design includes:
  - `postgres`
  - `tomcat`
  - frontend build container for the adjacent `../frontend` project
  - Nginx reverse/static server
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
| frontend build container | compose-defined | Builds the adjacent React frontend project. |
| Nginx | compose-defined | Serves frontend static assets and reverse-proxies backend API traffic. |
| `jenkins` | `cms-jenkins` | CI build/deploy runner. |

Important ports:

- PostgreSQL host port: `5433` mapped to container `5432`
- Tomcat host port: `8081` mapped to container `8080`
- Nginx host port: `8083`
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

- Root documentation is intentionally small:
  - `AGENTS.md` contains Codex agent operating rules.
  - `project.md` is the canonical stable project context.
  - `cluster-jdbc-todo.md` tracks the remaining JDBC cluster validation work.
- frontend handoff documents were removed from this repo after being transferred to the separate frontend repository
- keep `project.md` focused on stable project context and intended architecture
- keep machine-specific or temporary setup details out of this file unless they become permanent project conventions

Runtime URLs:

- local standalone Tomcat app context: `http://localhost:8080/cms-app`

Current cluster JDBC notes:

- the session store and rate limiter store are already externalized to PostgreSQL-backed implementations
- the remaining open points are operational and consistency related, not basic state externalization
- the canonical shortlist lives in `cluster-jdbc-todo.md`
- the main remaining risks are:
  - same-session concurrent writes
  - cleanup of expired session and limiter rows
  - multi-node end-to-end verification
  - user snapshot freshness after admin changes
  - DB load and index tuning
- Docker Tomcat root context: `http://localhost:8081`
- health endpoint when deployed under `/cms-app`: `http://localhost:8080/cms-app/hello`
- health endpoint in Docker root context: `http://localhost:8081/hello`

Frontend integration notes:

- Vite dev proxy can map `/api` to `http://localhost:8080/cms-app/api`
- backend CORS currently allows `http://localhost:5173` and `http://127.0.0.1:5173`
- even with CORS, same-origin/reverse-proxy deployment remains simpler for session auth
- frontend API client should centralize `credentials: "include"` and CSRF header handling

Testing notes:

- `mvn test` runs DB-backed DAO tests
- `mvn package` also runs tests unless skipped
- current verified test count: 123
- PostgreSQL schema comes from app startup migrations in `src/main/resources/db/migration/`
- tests clean up their own `dao_test_` user data and temporary boolean test table

---

## Workflow

- Work in small steps
- Implement feature parts separately:
  - model -> DAO -> service -> servlet
- Avoid large one-step implementations
- For durable architecture/project conventions, update this file.
