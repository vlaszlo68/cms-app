# Frontend Handoff

## Purpose

This file summarizes the current backend API contract and runtime assumptions that are relevant for a separate React frontend repository.

Status note:

- this file was created in the backend repo as the source handoff document
- it was also copied into the separate frontend repo for frontend-side work

Source of truth for this handoff:

- current backend source code under `src/main/java/hu/laci/cms/backend/`
- current Docker/Tomcat setup in `docker-compose.yml` and `docker/tomcat/Dockerfile`

## Backend Summary

- Stack: Java 21, Servlet API, JDBC, PostgreSQL, Tomcat 9
- Packaging: Maven WAR
- Auth model: session-based authentication
- JSON library: Gson
- Session key for authenticated user: `user`
- CSRF session key: `csrfToken`
- CSRF request header: `X-CSRF-Token`
- API responses use a common envelope:
  - success: `{ "success": true, "data": ... }`
  - error: `{ "success": false, "error": { "code": "...", "message": "..." } }`

The backend stores a password-hash-free `hu.laci.cms.backend.dto.auth.AuthenticatedUser` object in the HTTP session on successful login.
The backend also stores a CSRF token in the same session. The token is returned by successful login and `/api/auth/me` responses.

## API Base URL

The effective base URL depends on deployment mode.

### Local Tomcat manual deploy

If the WAR is deployed as `cms-app.war` into a standalone Tomcat:

- base app URL: `http://localhost:8081/cms-app`
- auth login URL: `http://localhost:8081/cms-app/api/auth/login`

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
VITE_API_BASE_URL=http://localhost:8081/cms-app
```

## Auth Endpoints

All auth endpoints return the common API response envelope.

Successful response shape:

```json
{
  "success": true,
  "data": {}
}
```

Error response shape:

```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "Human readable message"
  }
}
```

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
  "success": true,
  "data": {
    "id": 1,
    "loginName": "tester",
    "email": "tester@example.com",
    "csrfToken": "base64url-token"
  }
}
```

Invalid credentials:

- status: `401`

```json
{
  "success": false,
  "error": {
    "code": "INVALID_CREDENTIALS",
    "message": "Invalid credentials"
  }
}
```

Invalid or incomplete JSON:

- status: `400`

```json
{
  "success": false,
  "error": {
    "code": "INVALID_REQUEST",
    "message": "loginName and password are required."
  }
}
```

or

```json
{
  "success": false,
  "error": {
    "code": "INVALID_REQUEST",
    "message": "Invalid JSON request body."
  }
}
```

Behavior:

- on success the backend creates a session and stores the authenticated user under session attribute `user`
- the backend rotates the session id on successful login before storing the authenticated user
- the backend creates a session CSRF token and returns it in `data.csrfToken`

### `POST /api/auth/logout`

Request body:

- none required

Required header after login/session restore:

```http
X-CSRF-Token: <csrfToken>
```

Successful response:

- status: `200`

```json
{
  "success": true,
  "data": {
    "message": "Logged out"
  }
}
```

Behavior:

- invalidates the current session if one exists
- because logout is a state-changing request, it is protected by the CSRF filter

### `GET /api/auth/me`

Successful response:

- status: `200`

```json
{
  "success": true,
  "data": {
    "id": 1,
    "loginName": "tester",
    "email": "tester@example.com",
    "csrfToken": "base64url-token"
  }
}
```

Unauthenticated response expected by frontend:

- status: `401`

```json
{
  "success": false,
  "error": {
    "code": "AUTH_REQUIRED",
    "message": "Authentication required"
  }
}
```

Important:

- `/api/auth/me` is behind `AuthFilter`, so unauthenticated requests usually receive the filter-level response:
  - `401`
- `{"success":false,"error":{"code":"AUTH_REQUIRED","message":"Authentication required"}}`

## Protected API Behavior

There is an `AuthFilter` mapped to:

- `/api/*`

Public exceptions in code:

- `/api/auth/login`
- `/api/auth/logout`

All other `/api/*` endpoints currently require a valid session where the `user` attribute is an `AuthenticatedUser` object.

If there is no authenticated session, the filter returns:

- status: `401`
- content-type: `application/json`

```json
{
  "success": false,
  "error": {
    "code": "AUTH_REQUIRED",
    "message": "Authentication required"
  }
}
```

Frontend implication:

- any protected API call returning `401` should be treated as logged-out state

## CSRF Protection

State-changing `/api/*` requests are protected by `CsrfFilter`.

Protected HTTP methods:

- `POST`
- `PUT`
- `PATCH`
- `DELETE`

Not checked:

- `GET`
- `HEAD`
- `OPTIONS`
- `POST /api/auth/login`

Frontend requirements:

- store `data.csrfToken` from successful `login` and `me` responses
- send it on every state-changing API request:

```http
X-CSRF-Token: <csrfToken>
```

Invalid or missing token response:

- status: `403`

```json
{
  "success": false,
  "error": {
    "code": "CSRF_INVALID",
    "message": "Invalid CSRF token"
  }
}
```

On logout, clear the frontend auth state and the stored CSRF token.

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

For state-changing requests after login/session restore, also include the CSRF header:

```ts
fetch(`${API_BASE_URL}/api/auth/logout`, {
  method: 'POST',
  credentials: 'include',
  headers: {
    'X-CSRF-Token': csrfToken,
  },
})
```

## Current Known Constraints

### 1. CORS layer is implemented

The backend has a dedicated CORS filter.

Current allowed origins in `web.xml`:

- `http://localhost:5173`
- `http://127.0.0.1:5173`

Current allowed CORS methods:

- `GET`
- `POST`
- `PUT`
- `PATCH`
- `DELETE`
- `OPTIONS`

Credentialed CORS is enabled, so `credentials: 'include'` remains required for session auth.

Practical recommendation for frontend local development:

- the existing Vite dev proxy is still a good option
- direct browser calls from the listed origins are also supported

The backend also allows the `X-CSRF-Token` request header for CORS preflight.

### 2. AuthFilter public-path matching is context-path safe

Current filter code compares:

- `request.getServletPath()`

against exact strings:

- `/api/auth/login`
- `/api/auth/logout`

Implication:

- public auth endpoints work both at root context and when the app is deployed under `/cms-app`

## Recommended Frontend Auth Flow

1. On app startup call `GET /api/auth/me` with `credentials: 'include'`.
2. If response is `200`, hydrate frontend auth state from the returned JSON and store `data.csrfToken`.
3. If response is `401`, treat the user as logged out.
4. On login submit `POST /api/auth/login` with JSON body and `credentials: 'include'`.
5. Store `data.csrfToken` from login success.
6. On logout call `POST /api/auth/logout` with `credentials: 'include'` and `X-CSRF-Token`, then clear frontend auth state.
7. Add `X-CSRF-Token` to all future state-changing API calls.

## Suggested Frontend Env Variables

```env
VITE_API_BASE_URL=http://localhost:8081
```

If using standalone Tomcat instead:

```env
VITE_API_BASE_URL=http://localhost:8081/cms-app
```

## Test User Used During Backend Verification

The backend was locally verified with this user in PostgreSQL:

- `loginName`: `tester`
- `password`: `pw`
- `email`: `tester@example.com`

This is only a local development/test detail, not a product requirement.
