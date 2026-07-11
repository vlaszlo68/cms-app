---
name: cms-api-testing
description: Project-specific CMS backend API testing workflow for D:\java\cms_project\cms-app. Use when Codex is asked to test the app, run API smoke tests, verify backend endpoints, check auth/login/logout/session/CSRF/CAPTCHA behavior, test registration, test admin APIs, or run non-destructive or destructive CRUD API checks against the local Java Servlet CMS.
---

# CMS API Testing

Use this skill to test the CMS backend over HTTP. This skill is specific to the Java Servlet + JDBC backend in `D:\java\cms_project\cms-app`.

## Modes

When this skill is used, ask the user which mode to run before making API calls unless the user already chose a mode in the prompt.

- `non-destructive`: health, auth config, unauthenticated `/me`, login, authenticated `/me`, read-only admin endpoints, logout.
- `destructive`: all non-destructive checks plus create/read/update/delete checks for CRUD surfaces where the API exposes those operations. Require an explicit destructive request and use the script's `-ConfirmDestructive` guard.

Do not run destructive mode against shared, production, or unclear environments.

## Workflow

1. Ask: `Which API test mode should I run: non-destructive or destructive CRUD?` Skip this only when the user already specified the mode.
2. Confirm the app base URL. Use `http://localhost:8080/cms-app` for local standalone Tomcat.
3. Check whether the app is reachable before running the selected tests.
4. If the app is not reachable, ask the user whether to start it with `cms-local-runtime` before continuing. Do not start it silently.
5. For normal API testing, CAPTCHA should be disabled for both login and registration before the app starts. Use the currently supported project mechanism: set `captcha.login.enabled=false` and `captcha.registration.enabled=false` in `src/main/webapp/WEB-INF/web.xml` before packaging/startup through `cms-local-runtime`. Use environment variables only if the backend has a verified implementation for these exact CAPTCHA flags.
6. Check `GET /api/auth/config` before login and do not assume the current CAPTCHA state.
7. If CAPTCHA is enabled and the selected test mode needs login, session, admin, registration, or destructive coverage, ask the user whether to restart/start the app with CAPTCHA disabled through `cms-local-runtime`. Do not disable CAPTCHA or restart silently.
8. If CAPTCHA remains enabled, API tests can cover only public reachability, auth config, unauthenticated `/me`, and CAPTCHA image retrieval. Do not run login/session/admin/destructive flows unless CAPTCHA is disabled or the user explicitly provides a valid CAPTCHA-solving approach.
9. Use one `WebRequestSession` or equivalent cookie jar for login, authenticated calls, and logout.
10. Extract `data.csrfToken` from login or `/api/auth/me`.
11. Send `X-CSRF-Token` on state-changing authenticated calls.
12. Report endpoint, expected status, actual status, and pass/fail.
13. State whether destructive checks were skipped, run, cleaned up, or failed during cleanup.
14. If the API test flow started or restarted the app with CAPTCHA disabled, stop the app through `cms-local-runtime` at the end of testing so both CAPTCHA flags are restored to enabled. Do not leave startup-only CAPTCHA disablement in the repo.

## Script

Prefer the bundled PowerShell script for repeatable local testing:

```powershell
.agents\skills\cms-api-testing\scripts\smoke-api.ps1
```

Run destructive CRUD checks only with the guard:

```powershell
.agents\skills\cms-api-testing\scripts\smoke-api.ps1 -Mode Destructive -ConfirmDestructive
```

Useful options:

```powershell
.agents\skills\cms-api-testing\scripts\smoke-api.ps1 -BaseUrl "http://localhost:8080/cms-app" -LoginName tester -Password pw
```

The script uses the documented local admin `tester` / `pw` by default. The `tester` user is only for login. Never update, delete, deactivate, approve, reject, or otherwise mutate `tester`.

## Endpoint Reference

Read `references/api-test-matrix.md` when planning manual API tests, expanding the script, investigating a failed endpoint, or explaining what destructive mode covers.

## Safety Rules

- Never use destructive mode without explicit user intent.
- Keep test data identifiable with the `codex-api-test-*` prefix.
- In destructive mode, update and delete only database records that were created during the same API test run.
- Delete created test entities before logout.
- Do not update or delete existing rows as part of CRUD checks. Existing rows may be read only.
- Treat registration as destructive because it creates a pending user. Do not run it unless the user explicitly asks for registration coverage or destructive mode.
- During normal API testing, require CAPTCHA disabled for login and registration before app startup. In this project, that means using `cms-local-runtime` to set the two `web.xml` context params to `false` before build/start unless a verified environment-variable override exists.
- If the API test flow starts or restarts the app with CAPTCHA disabled, use `cms-local-runtime` stop/cleanup at the end so the CAPTCHA flags are restored to enabled.
- When CAPTCHA is enabled, do not try to bypass it. Full login/session/admin/destructive API testing requires CAPTCHA to be disabled or a user-approved valid CAPTCHA-solving approach. Without that, limit testing to public endpoints and CAPTCHA retrieval.
- Use Playwright MCP for browser UI verification; use direct API calls or the bundled script for backend API behavior.
