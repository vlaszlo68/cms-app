# CAPTCHA Frontend Handoff

This note is for the frontend coding agent implementing login and registration CAPTCHA UX against the Java backend.

## Backend Contract

- CAPTCHA endpoint: `GET /api/auth/captcha?purpose=login` or `GET /api/auth/captcha?purpose=registration`
- Response content type: `image/svg+xml`
- CAPTCHA id response header: `X-Captcha-Id`
- Requests must include credentials/cookies, because the expected answer is stored in the HTTP session.
- CORS exposes `X-Captcha-Id`.
- Missing or invalid `purpose` defaults to `registration`, so login must explicitly send `purpose=login`.

Example fetch:

```ts
const response = await fetch('/api/auth/captcha?purpose=login', {
  method: 'GET',
  credentials: 'include',
});

const captchaId = response.headers.get('X-Captcha-Id');
const svg = await response.text();
```

Render the returned SVG as an image/blob URL or sanitized inline SVG. Keep the `captchaId` paired with the visible challenge.

## Validation Rules

The backend enforces all security rules server-side:

- Challenge TTL: 3 minutes.
- Maximum validation attempts: 2 per challenge.
- Minimum solve time: 1 second after challenge generation.
- Purpose binding: a login challenge cannot be used for registration and a registration challenge cannot be used for login.
- CAPTCHA generation is rate-limited by IP + session.
- `captchaHoneypot` must be empty.

Frontend behavior should improve UX only. Do not rely on frontend timers or counters as security controls.

## Login Request

When `GET /api/auth/config` returns `loginCaptchaEnabled: true`, login must submit:

```json
{
  "loginName": "tester",
  "password": "pw",
  "captchaId": "value-from-X-Captcha-Id",
  "captchaAnswer": "10",
  "captchaHoneypot": ""
}
```

If CAPTCHA is disabled, omit or leave CAPTCHA fields empty.

## Registration Request

When `GET /api/auth/config` returns `registrationCaptchaEnabled: true`, registration must submit:

```json
{
  "loginName": "newuser",
  "userName": "New User",
  "emailAddress": "newuser@example.com",
  "password": "Password123!",
  "captchaId": "value-from-X-Captcha-Id",
  "captchaAnswer": "10",
  "captchaHoneypot": ""
}
```

If registration CAPTCHA is disabled, omit or leave CAPTCHA fields empty.

## Honeypot Field

Add a hidden field named `captchaHoneypot` to login and registration forms.

Requirements:

- Real users must not see or fill it.
- It should submit as an empty string.
- Do not use labels, autocomplete hints, or visible UI for it.
- Any non-empty value is rejected by the backend.

Example:

```tsx
<input
  type="text"
  name="captchaHoneypot"
  value={captchaHoneypot}
  onChange={(event) => setCaptchaHoneypot(event.target.value)}
  tabIndex={-1}
  autoComplete="off"
  aria-hidden="true"
  style={{ display: 'none' }}
/>
```

## Recommended UX

- Load a fresh CAPTCHA when the login/register view opens and CAPTCHA is enabled.
- Reload CAPTCHA after any `CAPTCHA_INVALID` response.
- Reload CAPTCHA after successful login/registration cleanup if the form remains mounted.
- Provide a refresh button next to the challenge image.
- Clear `captchaAnswer` whenever a new CAPTCHA is loaded.
- Disable submit until `captchaId` is present when CAPTCHA is enabled.
- If the user submits in less than 1 second, the backend rejects it; frontend may disable submit briefly for polish, but backend remains authoritative.
- Handle `429 RATE_LIMITED` or plain `429` from CAPTCHA generation with a short retry message.

## Error Handling

Relevant backend error codes:

- `CAPTCHA_INVALID`: answer invalid, challenge expired, too fast, wrong purpose, missing session, or attempt limit reached.
- `RATE_LIMITED`: too many registration attempts.
- CAPTCHA generation can return HTTP `429` with plain text `Too many CAPTCHA requests.`

On `CAPTCHA_INVALID`, do not keep the old challenge. Fetch a fresh one with the same purpose and clear the answer field.

## Implementation Notes

- Centralize CAPTCHA loading in a small helper/hook that accepts `purpose: 'login' | 'registration'`.
- Store `captchaId`, SVG/blob URL, `captchaAnswer`, loading state, and error state per form.
- Revoke old blob URLs when replacing CAPTCHA images.
- Always use `credentials: 'include'` for CAPTCHA, login, and registration requests.
- Do not share one CAPTCHA state between login and registration tabs/views.
