# Password Rules Frontend Handoff

## Backend Change

The public auth config endpoint now returns the full password policy needed by the frontend.

```http
GET /api/auth/config
```

Response shape:

```json
{
  "success": true,
  "data": {
    "loginCaptchaEnabled": true,
    "registrationCaptchaEnabled": true,
    "passwordPolicy": {
      "minLength": 2,
      "requireUppercase": false,
      "requireLowercase": false,
      "requireDigit": false,
      "requireSpecial": false
    }
  }
}
```

## Password Policy Fields

- `minLength`: minimum password length
- `requireUppercase`: requires at least one uppercase letter when true
- `requireLowercase`: requires at least one lowercase letter when true
- `requireDigit`: requires at least one digit when true
- `requireSpecial`: requires at least one non-letter, non-digit, non-whitespace character when true

If a rule is `false`, that character type is allowed but not required.

## Frontend Tasks

- Fetch `GET /api/auth/config` before rendering auth forms.
- Store `data.passwordPolicy` with the auth UI config.
- Use it to show password requirements on registration and password-change forms.
- Use it for client-side validation before submit.
- Still handle backend validation errors as source of truth.

Backend password validation errors still return:

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Password policy validation failed.",
    "validationErrors": ["TOO_SHORT", "MISSING_DIGIT"]
  }
}
```

Known validation error codes:

- `TOO_SHORT`
- `MISSING_UPPERCASE`
- `MISSING_LOWERCASE`
- `MISSING_DIGIT`
- `MISSING_SPECIAL`
