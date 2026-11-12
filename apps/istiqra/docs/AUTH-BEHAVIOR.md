# Auth and Session Runtime Behavior

Runtime behavior and policies for mobile authentication and session handling with `identity-service` and `exam-service`. Contract shapes and DTOs are generated directly from pinned OpenAPI specs (`contracts/`).

## Token Lifecycle

- Native authentication endpoints live under `/auth/native/*` (`NativeAuthenticationApi`).
- Tokens are returned in response payload (no cookies). `expiresIn` is in seconds.
- Refresh token rotation: Every successful `POST /auth/native/refresh` returns a fresh `refreshToken` alongside a new `accessToken`.
- Concurrent refresh race: When multiple requests trigger token refresh simultaneously, the backend issues a new `accessToken` but returns `refreshToken: null` to subsequent concurrent requests. The client preserves its existing valid refresh token.
- Logout (`POST /auth/native/logout`) is idempotent and does not fail for expired tokens.

## Device Binding and Roles

- Sessions are bound to `deviceId` passed during login and refresh.
- Attempting refresh with a mismatched `deviceId` yields HTTP `401` without modifying session state.
- Exam requests pass `X-Device-Id: <id>` header; requests missing the header return `400`, and sessions attached to another device return `409`.
- Accepted account roles: `STUDENT` (regular access) and `TEACHER` (supervisor verification to unlock exam or exit kiosk mode). Any other role receives `403`.

## Rate Limiting and Lockouts

- Invalid credentials return `401`.
- Repeated failed attempts trigger rate limiting (`429`).
- Exam token lockouts: 5 consecutive invalid exam access token submissions locks exam start attempts for 5 minutes (`429`).
