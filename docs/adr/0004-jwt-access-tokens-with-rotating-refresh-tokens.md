# 0004. Authenticate with short-lived JWT access tokens and rotating refresh tokens

- Status: Accepted
- Date: 2026-10-19

## Context

The API is consumed by a separate frontend (and tools like curl/Postman), not by server-rendered
pages ([ADR-0002](0002-serve-a-rest-only-json-api.md)). We need stateless authentication that scales
horizontally, works across origins, and lets users stay signed in for days without long-lived
credentials that can't be revoked.

## Decision

- **Access tokens**: JWTs signed with RS256 (our own RSA key pair), with a 15-minute lifetime,
  validated by Spring Security's OAuth2 Resource Server support ([#18](https://github.com/E-L-R/blog-backend/issues/18)). Claims: `sub` (user id),
  `preferred_username`, `roles`, `iss`, `iat`, `exp`.
- **Refresh tokens**: opaque 256-bit random strings, stored only as SHA-256 hashes, with a 7-day
  lifetime, **rotated** on every use ([#21](https://github.com/E-L-R/blog-backend/issues/21)). Reusing an already-rotated token revokes the whole token
  family (theft detection).
- Tokens travel in the `Authorization` header (`Bearer`), never in cookies. CSRF protection is
  therefore disabled: CSRF exploits the browser attaching cookies automatically, and it never
  attaches bearer tokens.

## Consequences

**Positive**

- The API is stateless: no session store, and any instance can validate any access token.
- Stolen access tokens expire quickly; stolen refresh tokens are detected when they are reused.
- Logout and password changes revoke refresh tokens. Access tokens remain valid for at most 15 minutes.

**Negative**

- Clients must store tokens securely and implement the refresh logic. XSS can read tokens kept in
  JavaScript-accessible storage: blog-frontend keeps the access token in memory but the refresh token
  in `localStorage`. [#87](https://github.com/E-L-R/blog-backend/issues/87) proposes an httpOnly cookie for the browser's refresh token. That would need
  CSRF protection on the refresh endpoint and a new ADR superseding this one.
- Strict rotation can't tell a stolen token from a retried request: if the response to a refresh
  gets lost, the client's retry looks like reuse and signs the user out everywhere ([#88](https://github.com/E-L-R/blog-backend/issues/88) proposes a
  short grace period).
- We operate an RSA key pair: generation, storage as a secret, rotation.

## Alternatives considered

- **Server sessions and cookies**: simple and instantly revocable, but it needs CSRF protection and
  either sticky sessions or a shared session store, and it's awkward across origins.
- **Long-lived JWTs only**: can't be revoked; a leaked token stays valid for days.
- **External identity provider** (Keycloak, Auth0): production-grade, but it hides exactly what this
  project wants to teach. Migrating later stays possible because the resource-server side doesn't change.
