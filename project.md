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
- Maven
- WAR packaging
- Deployment target: Tomcat 9

### Frontend

- React
- Same repository (separate folder)
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
   - Transaction handling

3. Servlet
   - HTTP handling
   - Session management
   - JSON input/output

---

## API

- REST + JSON
- Base path: `/api`

Examples:

- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/pages`
- `POST /api/pages`
- `PUT /api/pages/{id}`
- `DELETE /api/pages/{id}`

Current implementation note:

- the planned API base path remains `/api`
- the currently implemented auth endpoints are temporarily:
  - `POST /login`
  - `POST /logout`
- local Tomcat deploy path in current development is therefore:
  - `http://localhost:8080/cms-app/login`
  - `http://localhost:8080/cms-app/logout`
- the current DB connection code reads `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` first, then falls back to `web.xml` context params and finally hardcoded defaults

---

## Authentication

- Session-based
- Cookie handled by browser
- No JWT

---

## Database

- PostgreSQL
- Manual SQL (JDBC)
- Connection pool: HikariCP
- Prefer ANSI SQL where possible
- Use vendor-specific SQL only when justified
- Current connection entry point: `hu.laci.cms.backend.config.DatabaseConfig`

---

## Current Structure

cms/
|-- src/
|   |-- main/
|   |   |-- java/
|   |   |   `-- hu/laci/cms/
|   |   |       |-- model/
|   |   |       |-- dao/
|   |   |       |-- service/
|   |   |       |-- servlet/
|   |   |       `-- backend/config/
|   |   `-- webapp/
|-- docker/
|   |-- postgres/
|   `-- tomcat/
|-- skills/
|-- SESSION_CONTEXT.md
|-- docker-compose.yml
|-- pom.xml
|-- project.md
`-- agent.md

---

## Target Backend Structure

- `hu.laci.cms.model`
- `hu.laci.cms.dao`
- `hu.laci.cms.service`
- `hu.laci.cms.servlet`
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
- auth servlet layer is now implemented with:
  - `hu.laci.cms.servlet.AuthServlet`
  - `hu.laci.cms.servlet.LogoutServlet`
  - `hu.laci.cms.servlet.LoginRequest`
  - `hu.laci.cms.servlet.JsonServletSupport`
- JSON request/response handling currently uses Gson
- session-based authentication is active through `HttpSession`

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
- the current Java backend configuration already supports those environment variables
- `web.xml` still contains the Docker-oriented fallback JDBC host (`postgres`), so local non-Docker Tomcat runs need `DB_HOST=localhost` override

---

## Working Notes

- `SESSION_CONTEXT.md` stores the latest implementation summary and local runtime state for follow-up sessions
- keep `project.md` focused on stable project context and intended architecture
- keep machine-specific or temporary setup details out of this file unless they become permanent project conventions

---

## Workflow

- Work in small steps
- Implement feature parts separately:
  - model -> DAO -> service -> servlet
- Avoid large one-step implementations
