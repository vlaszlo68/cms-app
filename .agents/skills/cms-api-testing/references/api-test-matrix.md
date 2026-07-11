# CMS API Test Matrix

Base URL for local standalone Tomcat: `http://localhost:8080/cms-app`.

## Non-Destructive Mode

| Step | Request | Expected |
| --- | --- | --- |
| Health | `GET /hello` | `200`, body contains `Hello CMS` |
| Auth config | `GET /api/auth/config` | `200`, common envelope, CAPTCHA flags and password policy |
| Unauthenticated session | `GET /api/auth/me` | `401` |
| Login | `POST /api/auth/login` | `200`, `data.loginName`, `data.role`, `data.csrfToken` |
| Session restore | `GET /api/auth/me` with session cookie | `200`, same auth shape as login |
| Pages read | `GET /api/pages` | `200` for admin |
| Templates read | `GET /api/templates` | `200` for admin |
| Menus read | `GET /api/menus` | `200` for admin |
| Media metadata read | `GET /api/media` | `200` for admin |
| Site settings read | `GET /api/site-settings` | `200` for admin |
| Logout | `POST /api/auth/logout` with `X-CSRF-Token` | `200`, message `Logged out` |

## Destructive Mode

Run only when the user explicitly requests destructive CRUD checks.

The script should create test entities with a unique `codex-api-test-*` prefix and update/delete only those same entities. The `tester` user is only for login and must never be updated, deleted, deactivated, approved, rejected, or otherwise mutated.

| Surface | Create | Read | Update | Delete/Cleanup | Notes |
| --- | --- | --- | --- | --- | --- |
| Users | `POST /api/users` | `GET /api/users/{id}` | `PUT /api/users/{id}` | `DELETE /api/users/{id}` | Admin only, use `USER` role for created test users |
| Pages | `POST /api/pages` | `GET /api/pages/{id}` | `PUT /api/pages/{id}` | `DELETE /api/pages/{id}` | Use `BLOCK` when also testing page blocks |
| Page blocks | `POST /api/page-blocks` | `GET /api/page-blocks/{id}` | `PUT /api/page-blocks/{id}` | `DELETE /api/page-blocks/{id}` | Requires a page id |
| Menus | `POST /api/menus` | `GET /api/menus/{id}` | `PUT /api/menus/{id}` | `DELETE /api/menus/{id}` | Deleting a menu deletes its items |
| Menu items | `POST /api/menu-items` | `GET /api/menus/{menuId}/items` | `PUT /api/menu-items/{id}` | `DELETE /api/menu-items/{id}` | Prefer `URL` target to avoid page coupling |
| Templates | `POST /api/templates` | `GET /api/templates/{id}` | `PUT /api/templates/{id}` | `DELETE /api/templates/{id}` | Delete deactivates templates |
| Media | `POST /api/media` multipart | `GET /api/media/{id}` and `/content` | Not exposed | `DELETE /api/media/{id}` | API exposes create/read/delete, not update |
| Site settings | Not exposed | `GET /api/site-settings` | Not run by default | Not run by default | Singleton settings record, not normal CRUD; do not update it during standard destructive mode because the row was not created by the test |

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
