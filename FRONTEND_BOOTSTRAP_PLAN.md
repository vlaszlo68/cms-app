# Frontend Bootstrap Plan

## Goal

Create a separate React frontend repository for the CMS backend described in `FRONTEND_HANDOFF.md`.

Status note:

- this plan was created in the backend repo
- it was also copied into the separate frontend repo as the initial frontend bootstrap reference

Recommended location:

```text
D:\java\IdeaProjects\
  cms-app\
  cms-frontend\
```

The frontend should be developed as an independent repository with its own Git history, Node toolchain, build pipeline, and deployment flow.

## Recommended Stack

- React 19
- TypeScript
- Vite
- React Router
- plain `fetch` for HTTP
- session-based auth using browser cookies

Optional but reasonable later additions:

- ESLint
- Prettier
- Vitest
- React Testing Library

## Repo Structure

```text
cms-frontend/
  public/
  src/
    api/
      authApi.ts
      httpClient.ts
    app/
      providers.tsx
      router.tsx
    components/
      common/
      layout/
    features/
      auth/
        components/
        hooks/
        pages/
        types.ts
      dashboard/
        components/
        pages/
    hooks/
    lib/
    styles/
      globals.css
      variables.css
    types/
    App.tsx
    main.tsx
  .env.example
  .gitignore
  eslint.config.js
  index.html
  package.json
  README.md
  tsconfig.json
  vite.config.ts
```

## Initial App Scope

The first usable frontend milestone should only solve auth and app shell concerns:

1. Login page
2. Session restore on app startup through `/api/auth/me`
3. Logout action
4. Protected route shell
5. Placeholder dashboard page after login

Do not start with full CMS functionality. First make auth integration stable.

## Environment Variables

Recommended `.env.example`:

```env
VITE_API_BASE_URL=http://localhost:8081
```

For standalone Tomcat instead of Docker:

```env
VITE_API_BASE_URL=http://localhost:8080/cms-app
```

## API Layer Design

### `src/api/httpClient.ts`

Responsibilities:

- prepend `VITE_API_BASE_URL`
- set `credentials: 'include'`
- set `Content-Type: application/json` for JSON requests
- parse JSON responses
- normalize `401` handling

Suggested shape:

- `get<T>(path: string)`
- `post<T>(path: string, body?: unknown)`

### `src/api/authApi.ts`

Expose:

- `login(input)`
- `logout()`
- `me()`

Suggested request/response types:

```ts
export type LoginRequest = {
  loginName: string;
  password: string;
};

export type AuthUser = {
  id: number;
  loginName: string;
  email: string;
};
```

## Auth State Design

Suggested minimal auth state:

```ts
type AuthState = {
  user: AuthUser | null;
  isLoading: boolean;
  isAuthenticated: boolean;
};
```

Recommended behavior:

- app start:
  - call `me()`
  - `200` -> set authenticated user
  - `401` -> set logged-out state
- login success:
  - store returned user in memory state
- logout success:
  - clear auth state
- protected API `401`:
  - clear auth state
  - redirect to login

Keep auth state in React context first. No need for Redux or heavier state libraries at this stage.

## Routing

Suggested routes:

- `/login`
- `/`

Behavior:

- `/login`
  - if already authenticated, redirect to `/`
- `/`
  - protected route
  - render dashboard placeholder

Suggested files:

- `src/app/router.tsx`
- `src/features/auth/pages/LoginPage.tsx`
- `src/features/dashboard/pages/DashboardPage.tsx`

## Local Dev Integration

## Preferred mode

Use the backend through Docker/root context during frontend development when possible:

- backend app: `http://localhost:8081`

Reason:

- current backend `AuthFilter` public path matching is safer in root-context deployment than in `/cms-app`

## Dev proxy recommendation

Because the backend currently has no dedicated CORS support, use a Vite proxy.

Example direction for `vite.config.ts`:

- frontend dev server: `http://localhost:5173`
- proxy `/api` to `http://localhost:8081`

If you also need non-API backend resources later, proxy them explicitly.

## UI Composition Recommendation

Keep the first UI intentionally small and functional:

- centered login card
- clear error banner for `401`
- loading state during session restore
- simple authenticated shell with top bar and logout button

Do not overbuild component libraries before the auth flow is proven.

## Suggested File Responsibilities

### `src/features/auth/pages/LoginPage.tsx`

- login form
- submit handler
- field-level local state
- render backend auth errors

### `src/features/auth/hooks/useAuth.ts`

- expose auth context
- helpers like `login`, `logout`, `refreshUser`

### `src/app/providers.tsx`

- auth provider
- router provider wrapper if needed

### `src/components/layout/AppShell.tsx`

- authenticated layout
- header
- logout button
- content outlet

## Bootstrap Sequence

1. Create `cms-frontend` repo in a separate directory.
2. Initialize Vite React TypeScript app.
3. Add router and linting.
4. Create `.env.example`.
5. Implement `httpClient.ts`.
6. Implement `authApi.ts`.
7. Implement auth context and startup session restore.
8. Implement login page.
9. Implement protected route wrapper.
10. Implement logout action.
11. Verify full flow against backend:
    - unauthenticated `me`
    - login success
    - refresh after login
    - logout success
    - protected route redirect

## Recommended First Deliverable

The first frontend PR/repo milestone should include:

- project scaffold
- auth API integration
- login page
- protected dashboard placeholder
- logout button
- Vite proxy config
- README with startup instructions

## README Checklist For Frontend Repo

The new frontend repo should document:

- required Node version
- install command
- run command
- env variables
- backend dependency
- local proxy behavior
- current implemented routes
- auth/session notes

## Risks To Keep Visible

### 1. Backend CORS is not implemented

Without proxy or backend CORS work, browser requests from a separate dev origin may fail.

### 2. Backend auth uses session cookies

Every request that depends on authentication must send credentials.

### 3. Current backend filter path logic is deployment-sensitive

Root-context backend is safer for frontend development until that logic is normalized.

## Recommended Next Step

When ready to switch repos:

1. create the `cms-frontend` directory
2. initialize the React app there
3. copy `FRONTEND_HANDOFF.md` into the new repo or keep it open as reference
4. implement auth-only MVP first
