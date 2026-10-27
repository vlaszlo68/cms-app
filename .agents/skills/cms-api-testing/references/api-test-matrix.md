# CMS API Test Matrix

Base URL for local standalone Tomcat: `http://localhost:8080/cms-app`.

The migrated development Site code is `default`. Every Site-owned request carries a Site code explicitly; the smoke script accepts `-SiteCode` and defaults it to `default`.

## Strictness

Standard checks verify expected HTTP status codes and the basic login/session/CSRF flow.

Strict checks are optional and must be selected before the test starts. Strict mode adds response-body assertions and, for destructive mode, post-cleanup read checks proving test-created records are no longer readable.

## Non-Destructive Mode

| Step | Request | Expected |
| --- | --- | --- |
| Health | `GET /hello` | `200`, body contains `Hello CMS` |
| Auth config | `GET /api/auth/config` | `200`, common envelope, CAPTCHA flags and password policy |
| Unauthenticated session | `GET /api/auth/me` | `401` |
| Login | `POST /api/auth/login` | `200`, `data.loginName`, `data.role`, `data.csrfToken` |
| Session restore | `GET /api/auth/me` with session cookie | `200`, same auth shape as login |
| Sites read | `GET /api/admin/sites` | `200` for admin, including inactive Sites |
| Pages read | `GET /api/admin/sites/{siteCode}/pages` | `200` for admin |
| Templates read | `GET /api/templates` | `200` for admin |
| Menus read | `GET /api/admin/sites/{siteCode}/menus` | `200` for admin |
| Media metadata read | `GET /api/admin/sites/{siteCode}/media` | `200` for admin |
| Site settings read | `GET /api/admin/sites/{siteCode}/site-settings` | `200` for admin |
| Logout | `POST /api/auth/logout` with `X-CSRF-Token` | `200`, message `Logged out` |

Strict non-destructive assertions:

| Assertion | Expected |
| --- | --- |
| Health body | contains `Hello CMS` |
| Auth config envelope | `success=true`, CAPTCHA flags present, password policy present |
| Unauthenticated `/me` envelope | `success=false`, error object present |
| Login body | `success=true`, matching `data.loginName`, `data.role=ADMIN`, non-empty `data.csrfToken` |
| Session restore body | `success=true`, matching authenticated user shape, non-empty `data.csrfToken` |
| Admin list bodies | common success envelope, list-like `data` where applicable |
| Logout body | `success=true`, `data.message=Logged out` |

## Public Page Read

`GET /api/public/sites/{siteCode}/pages/{slug}` is unauthenticated and performs an exact, case-sensitive slug lookup. It returns a common success envelope only for a `PUBLISHED` `CONTENT` or `BLOCK` page. A CONTENT `data` object contains exactly `id`, `title`, `slug`, `pageType`, `templateCode`, and `content`; it omits `blocks`. A BLOCK `data` object contains exactly `id`, `title`, `slug`, `pageType`, `templateCode`, and a non-null `blocks` list; it omits `content`. Each public block contains `id`, `blockType`, `title`, `sortOrder`, `visible`, and `configJson`, never `pageId`; only visible blocks are returned in `sortOrder`, then id order, so every returned `visible` value is `true`. Unknown, draft, and archived pages return `404` with `PAGE_NOT_FOUND`. Missing or multi-segment slug paths return `400` with `INVALID_REQUEST`.

`GET /api/public/sites/{siteCode}/site-settings` is unauthenticated and returns the selected Site's common success envelope with exactly `siteName`, `logoMediaId`, `footerText`, `contactEmail`, `phone`, `facebookUrl`, and `linkedinUrl`. If that Site's settings record is absent, every field is present with a `null` value and the request does not create a record.

`GET /api/public/sites/{siteCode}/menus/MAIN` and `GET /api/public/sites/{siteCode}/menus/FOOTER` are unauthenticated and return active, visible, ordered menu trees. PAGE items contain `id`, `title`, `targetType`, `pageId`, `pageSlug`, `path`, and `children`; only PUBLISHED CONTENT page targets are included. URL items contain `id`, `title`, `targetType`, `targetUrl`, and `children`. Inactive or unknown menu codes return `404 MENU_NOT_FOUND`.

Unknown Site codes and inactive Sites return `404 SITE_NOT_FOUND` for public page, menu, and settings routes. Public routing never selects a Site from a session, host name, first row, or implicit fallback.

The repeatable smoke script verifies this endpoint in destructive mode using a published `CONTENT` fixture created within the same run, then deletes that fixture during cleanup. Strict destructive mode additionally verifies the limited field set and the public `404` after cleanup.

## Destructive Mode

Run only when the user explicitly requests destructive CRUD checks.

The script should create test entities with a unique `codex-api-test-*` prefix and update/delete only those same entities. The `tester` user is only for login and must never be updated, deleted, deactivated, approved, rejected, or otherwise mutated.

| Surface | Create | Read | Update | Delete/Cleanup | Notes |
| --- | --- | --- | --- | --- | --- |
| Users | `POST /api/users` | `GET /api/users/{id}` | `PUT /api/users/{id}` | API `DELETE`, then permanent database removal by current-run ID | Admin only, use `USER` role for created test users; API delete soft-deactivates, so the test script must physically remove only its own row |
| Pages | `POST /api/admin/sites/{siteCode}/pages` | `GET /api/admin/sites/{siteCode}/pages/{id}` | `PUT /api/admin/sites/{siteCode}/pages/{id}` | `DELETE /api/admin/sites/{siteCode}/pages/{id}` | Use `BLOCK` when also testing page blocks |
| Page blocks | `POST /api/admin/sites/{siteCode}/pages/{pageId}/blocks` | `GET /api/admin/sites/{siteCode}/pages/{pageId}/blocks/{id}` | `PUT /api/admin/sites/{siteCode}/pages/{pageId}/blocks/{id}` | `DELETE /api/admin/sites/{siteCode}/pages/{pageId}/blocks/{id}` | Parent Page and block id are both ownership-checked |
| Menus | `POST /api/admin/sites/{siteCode}/menus` | `GET /api/admin/sites/{siteCode}/menus/{id}` | `PUT /api/admin/sites/{siteCode}/menus/{id}` | `DELETE /api/admin/sites/{siteCode}/menus/{id}` | Deleting a menu deletes its items |
| Menu items | `POST /api/admin/sites/{siteCode}/menus/{menuId}/items` | `GET /api/admin/sites/{siteCode}/menus/{menuId}/items` | `PUT /api/admin/sites/{siteCode}/menus/{menuId}/items/{id}` | `DELETE /api/admin/sites/{siteCode}/menus/{menuId}/items/{id}` | Prefer `URL` target to avoid page coupling |
| Templates | `POST /api/templates` | `GET /api/templates/{id}` | `PUT /api/templates/{id}` | API `DELETE`, then permanent database removal by current-run ID | API delete deactivates templates, so the test script must physically remove only its own row after verifying no page reference |
| Media | `POST /api/admin/sites/{siteCode}/media` multipart | `GET /api/admin/sites/{siteCode}/media/{id}` and `/content` | Not exposed | `DELETE /api/admin/sites/{siteCode}/media/{id}` | API exposes create/read/delete, not update |
| Site settings | Not exposed | `GET /api/admin/sites/{siteCode}/site-settings` | Not run by default | Not run by default | One settings row per Site; do not update existing settings during standard destructive mode |

Site management is `GET/POST /api/admin/sites` and `GET/PUT /api/admin/sites/{siteCode}`. There is intentionally no Site delete endpoint. The live smoke script reads Sites but does not create disposable Sites; two-Site isolation fixtures belong in automated tests or an isolated disposable database.

Strict destructive assertions:

| Assertion | Expected |
| --- | --- |
| Created entity bodies | common success envelope and `data.id` present |
| Read-after-create bodies | returned `data.id` matches the created id |
| Update bodies | returned `data.id` matches the updated id, and updated fields match where the endpoint returns them |
| Media content | response body is non-empty |
| Cleanup verification | after delete, Site-scoped Page, nested PageBlock, Menu, and Media reads return `404`; deleted menu item is absent from its Site-scoped Menu item list; user and template API deletes first return `data.active=false`, then the script permanently removes only the current-run IDs and verifies their database row counts are zero |

## Auth And CAPTCHA Rules

- Login and `/api/auth/me` return `data.csrfToken`.
- State-changing authenticated calls require `X-CSRF-Token`.
- `POST /api/auth/login` and `POST /api/auth/register` are CSRF-exempt.
- `GET /api/auth/captcha?purpose=login|registration` returns SVG and `X-Captcha-Id`.
- For normal API testing, CAPTCHA should be disabled before the app starts. In this project, use `cms-local-runtime` to set `captcha.login.enabled=false` and `captcha.registration.enabled=false` in `src/main/webapp/WEB-INF/web.xml` before packaging/startup.
- Use CAPTCHA environment variables only if backend support for those exact flags has been verified.
- If the API test flow started or restarted the app with CAPTCHA disabled, stop/clean up through `cms-local-runtime` at the end so CAPTCHA is restored to enabled.
- If `loginCaptchaEnabled=true`, automated login cannot proceed without solving CAPTCHA. Ask before restarting/starting with CAPTCHA disabled; do not disable CAPTCHA silently.
- If `registrationCaptchaEnabled=true`, registration tests need a CAPTCHA challenge and answer; do not guess or bypass it.

## Request Payload Examples

Login:

```json
{
  "loginName": "tester",
  "password": "pw",
  "captchaHoneypot": ""
}
```

Page:

```json
{
  "title": "codex-api-test page",
  "slug": "codex-api-test-page",
  "content": "Temporary API test page",
  "pageType": "BLOCK",
  "status": "DRAFT",
  "metaTitle": "codex-api-test page",
  "metaDescription": "Temporary API test page",
  "homepage": false,
  "menuVisible": false,
  "templateId": null
}
```

Menu item with URL target:

```json
{
  "menuId": 123,
  "parentId": null,
  "pageId": null,
  "targetType": "URL",
  "targetUrl": "https://example.com/codex-api-test",
  "title": "codex-api-test link",
  "sortOrder": 1,
  "visible": true
}
```

## Application Platform Foundation

Application administration is Site-scoped and requires the normal authenticated ADMIN session plus CSRF token:

- `GET|POST /api/admin/sites/{siteCode}/applications`
- `GET|PUT /api/admin/sites/{siteCode}/applications/{applicationId}`

Application create/update payload: `{ "code": "SUPPORTED_CODE", "name": "Display name", "active": true }`.
The code must be explicitly supported by the deployed application-definition registry; uniqueness is `(site_id, code)`.

Menu item payloads accept `targetType: "APPLICATION"` with an `applicationId`. The application must be active and belong to the Site in the route. Cross-Site IDs must be rejected. Existing PAGE and URL target payloads remain valid.

Anonymous public application checks:

- `GET /api/public/sites/{siteCode}/applications/{applicationCode}` returns `{ success, data: { code, name } }` only for active Site, active Site Application, and supported code.
- `POST /api/public/sites/{siteCode}/applications/{applicationCode}/functions/{functionCode}` accepts a JSON object and returns the common envelope. It is intentionally CSRF-exempt only for this exact anonymous public function path; the Site/Application/function registry whitelist remains mandatory.

Do not run creation or dispatch checks against a shared runtime unless a test-only supported application definition and handler are explicitly available. No production demo function is part of this foundation.
