# Frontend Handoff

## Purpose

This file summarizes the current backend API contract and runtime assumptions that are relevant for a separate React frontend repository.

Status note:

- this file was created in the backend repo as the source handoff document
- it was also copied into the separate frontend repo for frontend-side work

Source of truth for this handoff:

- current backend source code under `src/main/java/hu/laci/cms/servlet/`
- current Docker/Tomcat setup in `docker-compose.yml` and `docker/tomcat/Dockerfile`

## Backend Summary

- Stack: Java 21, Servlet API, JDBC, PostgreSQL, Tomcat 9
- Packaging: Maven WAR
- Auth model: session-based authentication
- JSON library: Gson
- Session key for authenticated user: `user`

The backend stores the full `hu.laci.cms.model.User` object in the HTTP session on successful login.

## API Base URL

The effective base URL depends on deployment mode.

### Local Tomcat manual deploy

If the WAR is deployed as `cms-app.war` into a standalone Tomcat:

- base app URL: `http://localhost:8080/cms-app`
- auth login URL: `http://localhost:8080/cms-app/api/auth/login`

### Docker Tomcat deploy

The Docker image copies the WAR as `ROOT.war`, so the app runs on the root context:

- base app URL: `http://localhost:8081`
- auth login URL: `http://localhost:8081/api/auth/login`

For frontend environment variables, it is better to store the full backend base URL, for example:

```env
VITE_API_BASE_URL=http://localhost:8081
```

or for local standalone Tomcat:

```env
VITE_API_BASE_URL=http://localhost:8080/cms-app
```

## Auth Endpoints

### `POST /api/auth/login`

Request body:

```json
{
  "loginName": "string",
  "password": "string"
}
```

Successful response:

- status: `200`
- content-type: `application/json`

```json
{
  "id": 1,
  "loginName": "tester",
  "email": "tester@example.com"
}
```

Invalid credentials:

- status: `401`

```json
{
  "error": "Invalid credentials"
}
```

Invalid or incomplete JSON:

- status: `400`

```json
{
  "error": "loginName and password are required."
}
```

or

```json
{
  "error": "Invalid JSON request body."
}
```

Behavior:

- on success the backend creates a session and stores the authenticated user under session attribute `user`

### `POST /api/auth/logout`

Request body:

- none required

Successful response:

- status: `200`

```json
{
  "message": "Logged out"
}
```

Behavior:

- invalidates the current session using `request.getSession().invalidate()`

### `GET /api/auth/me`

Successful response:

- status: `200`

```json
{
  "id": 1,
  "loginName": "tester",
  "email": "tester@example.com"
}
```

Unauthenticated response expected by frontend:

- status: `401`

```json
{
  "error": "Unauthorized"
}
```

Important:

- the `MeServlet` itself contains a `"Not authenticated"` branch
- however, because `/api/auth/me` is behind `AuthFilter`, the frontend should currently expect the filter-level response:
  - `401`
  - `{"error":"Unauthorized"}`

## Protected API Behavior

There is an `AuthFilter` mapped to:

- `/api/*`

Public exceptions in code:

- `/api/auth/login`
- `/api/auth/logout`

All other `/api/*` endpoints currently require a valid session with a non-null `user` attribute.

If there is no authenticated session, the filter returns:

- status: `401`
- content-type: `application/json`

```json
{
  "error": "Unauthorized"
}
```

Frontend implication:

- any protected API call returning `401` should be treated as logged-out state

## Cookies and Frontend Fetching

Because authentication is session-based, the frontend must send cookies on every authenticated request.

Recommended fetch usage:

```ts
fetch(`${API_BASE_URL}/api/auth/me`, {
  method: 'GET',
  credentials: 'include',
})
```

The same applies to:

- login
- logout
- every protected `/api/*` request

## Current Known Constraints

### 1. No CORS layer is implemented yet

There is currently no dedicated CORS handling in the backend codebase.

Implication:

- if the React dev server runs on a different origin, direct browser calls may fail without backend CORS work

Practical recommendation for frontend local development:

- prefer a dev proxy from the React app to the backend instead of cross-origin browser calls

Example direction:

- React dev server on `localhost:5173`
- proxy `/api` to `http://localhost:8081` or `http://localhost:8080/cms-app`

### 2. AuthFilter public-path matching is context-path sensitive

Current filter code compares:

- `request.getRequestURI()`

against exact strings:

- `/api/auth/login`
- `/api/auth/logout`

Implication:

- this works as expected when the app is deployed at root context, for example Docker `ROOT.war`
- this may fail when the app is deployed under `/cms-app`, because the request URI then becomes:
  - `/cms-app/api/auth/login`
  - `/cms-app/api/auth/logout`

Frontend implication:

- for the cleanest frontend integration, prefer the Docker/root-context backend during frontend development until this filter logic is normalized

## Recommended Frontend Auth Flow

1. On app startup call `GET /api/auth/me` with `credentials: 'include'`.
2. If response is `200`, hydrate frontend auth state from the returned JSON.
3. If response is `401`, treat the user as logged out.
4. On login submit `POST /api/auth/login` with JSON body and `credentials: 'include'`.
5. On logout call `POST /api/auth/logout` with `credentials: 'include'`, then clear frontend auth state.

## Suggested Frontend Env Variables

```env
VITE_API_BASE_URL=http://localhost:8081
```

If using standalone Tomcat instead:

```env
VITE_API_BASE_URL=http://localhost:8080/cms-app
```

## Test User Used During Backend Verification

The backend was locally verified with this user in PostgreSQL:

- `loginName`: `tester`
- `password`: `pw`
- `email`: `tester@example.com`

This is only a local development/test detail, not a product requirement.
