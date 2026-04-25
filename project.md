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
- Maven
- WAR packaging
- Deployment: Tomcat 9 (Docker)

### Frontend
- React
- Same repository (separate folder)
- Communicates via REST API

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
- Base path: /api

Examples:
- POST   /api/auth/login
- POST   /api/auth/logout
- GET    /api/pages
- POST   /api/pages
- PUT    /api/pages/{id}
- DELETE /api/pages/{id}

Current implementation note:

- the planned API base path remains `/api`
- the currently implemented login endpoint is temporarily `POST /login`
- Tomcat deploy path in local development is therefore `http://localhost:8080/cms-app/login`

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
|   |   |       `-- Main.java
|   |   `-- webapp/
|-- skills/
|-- SESSION_CONTEXT.md
|-- target/
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

## Current Backend Status

- `User`, `UserDao`, `UserDaoImpl`, `AuthService`, `AuthServiceException`, `DatabaseConfig` already exist
- login servlet layer is now implemented with:
  - `hu.laci.cms.servlet.AuthServlet`
  - `hu.laci.cms.servlet.LoginRequest`
- JSON request parsing currently uses Jackson `ObjectMapper`
- session-based authentication is active through `HttpSession`

## Working Notes

- `SESSION_CONTEXT.md` stores the latest implementation summary and local runtime state for follow-up sessions
- keep `project.md` focused on stable project context and intended architecture
- keep machine-specific or temporary setup details out of this file unless they become permanent project conventions

---

## Workflow

- Work in small steps
- Implement feature parts separately:
  model -> DAO -> service -> servlet
- Avoid large one-step implementations
