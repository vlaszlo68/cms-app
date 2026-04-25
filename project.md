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
- Deployment: Tomcat (Docker)

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

---

## Project Structure

cms/
├── backend/
├── frontend/
├── docker/
└── agent.md

---

## Development Principles

- No frameworks (no Spring)
- No ORM
- Keep code simple and explicit
- Respect layer boundaries
- Avoid unnecessary abstractions

---

## Workflow

- Work in small steps
- Implement feature parts separately:
  model → DAO → service → servlet
- Avoid large one-step implementations
