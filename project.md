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

- current auth endpoints are implemented under `/api/auth/*`
  - `POST /api/auth/login`
  - `POST /api/auth/logout`
  - `GET /api/auth/me`
- local standalone Tomcat deploy path in current development is:
  - `http://localhost:8080/cms-app/api/auth/login`
  - `http://localhost:8080/cms-app/api/auth/logout`
  - `http://localhost:8080/cms-app/api/auth/me`
- Docker Tomcat deploy path in current development is:
  - `http://localhost:8081/api/auth/login`
  - `http://localhost:8081/api/auth/logout`
  - `http://localhost:8081/api/auth/me`
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
|-- FRONTEND_BOOTSTRAP_PLAN.md
|-- FRONTEND_HANDOFF.md
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
  - `hu.laci.cms.servlet.MeServlet`
  - `hu.laci.cms.servlet.LoginRequest`
  - `hu.laci.cms.servlet.JsonServletSupport`
  - `hu.laci.cms.servlet.AuthFilter`
- JSON request/response handling currently uses Gson
- session-based authentication is active through `HttpSession`
- a frontend handoff and bootstrap planning documents are maintained in this repo and were copied into the separate frontend repo for frontend-side work

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
