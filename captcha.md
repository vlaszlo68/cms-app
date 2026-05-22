# CAPTCHA Hardening Notes

Future improvements for the self-hosted SVG CAPTCHA, without using an external CAPTCHA provider.

## Goal

The current CAPTCHA is intended to slow down simple automated login and registration attempts. It is not meant to defeat advanced bots. These ideas can raise the cost of automation while keeping the project framework-light and self-contained.

## Candidate Improvements

- Implemented: expiration time is 3 minutes.
- Implemented: each challenge allows 2 validation attempts.
- Implemented: minimum solve time is 1 second.
- Implemented: CAPTCHA generation is rate limited by IP/session.
- Implemented: CAPTCHA context is bound to intended use through `purpose=login` or `purpose=registration`.
- Implemented: challenge variants include:
  - `2 + 7 - 3`
  - `3 * 4`
  - `three + 7`
  - `? + 5 = 12`
- Implemented: SVG variation includes:
  - character-specific baseline shifts
  - random font sizes
  - random rotations
  - crossing lines over glyphs
  - denser background grid/noise
  - random faint shapes behind text
- Implemented: login and registration reject non-empty `captchaHoneypot` values.

## Implemented Scope

The current implementation covers the recommended backend hardening steps from this note:

1. CAPTCHA TTL.
2. Per-challenge attempt counter.
3. Minimum solve time.
4. CAPTCHA generation rate limiting.
5. Richer math challenge variants.

Keep all validation server-side. Frontend behavior should only improve UX, never replace backend checks.
