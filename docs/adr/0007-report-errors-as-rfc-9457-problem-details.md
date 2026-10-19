# 0007. Report every error as an RFC 9457 problem detail

- Status: Accepted
- Date: 2026-10-19

## Context

Errors come from many places: bean validation, missing resources, business rules, optimistic-locking
conflicts, Spring Security's filters (before any controller runs), rate limiting and request size
limits. By default each of them answers in a different shape: Spring's error JSON, an empty body,
or an HTML page.

Clients need to react to errors reliably: show field messages next to inputs, refresh a token after a
401, offer "reload" after a conflict. They can't do that by parsing human-readable messages that
change with every rewording.

## Decision

Every error response is **`application/problem+json`** as defined by RFC 9457 ([#13](https://github.com/E-L-R/blog-backend/issues/13)):

- Members: `type`, `title`, `status`, `detail` and `instance`, plus `errors[]` (`{field, message}`)
  for validation errors. Every response, errors included, also carries the request's trace id in the
  `X-Trace-Id` header ([#49](https://github.com/E-L-R/blog-backend/issues/49)), so a user's error report can be matched to the server log.
- **`type` is the stable contract.** It is a URI pointing at its explanation in
  [`docs/problems.md`](../problems.md) (for example `…/docs/problems.md#conflict`). Clients branch on
  `type`, never on `title` or `detail`. A `title` is fixed per type, and `detail` is free-form.
- One `@RestControllerAdvice` handles errors raised inside Spring MVC. The security handlers (401/403),
  rate limiting ([#46](https://github.com/E-L-R/blog-backend/issues/46)) and request size limits ([#47](https://github.com/E-L-R/blog-backend/issues/47)) write the same format, so every failure looks the same.
- A **500 never leaks internals**: no stack traces, exception messages or SQL. The details go to the
  server log, under the trace id.

## Consequences

**Positive**

- One error format for every endpoint and every failure, which clients handle in one place.
- Clients (blog-frontend among them) branch on stable `type` URIs, and the URIs document themselves.
- It's a standard: Spring Framework supports it natively (`ProblemDetail`, `ErrorResponse`), and
  generic HTTP clients and tools understand it.

**Negative**

- Every new kind of error needs a type, a documented section in `docs/problems.md`, and tests that
  assert on `type` and `status`.
- Errors produced outside Spring MVC (filters, security) need their own code to write the same format.
- Spring MVC's own errors (unknown route, 405, 415, malformed JSON, unsupported API version) keep
  `"type": "about:blank"`, so clients need a fallback to the status code for them.
- The `type` URIs point at this repository's `main` branch, so the documentation must move with them
  if the repository is ever renamed.

## Alternatives considered

- **Spring Boot's default error body** (`timestamp`, `status`, `error`, `path`): not a standard, has no
  stable machine-readable error kind, and differs between MVC and security errors.
- **A custom error envelope** (`{code, message}`): works, but it reinvents RFC 9457 without the tooling
  support or the documented semantics.
- **Status codes only, with empty bodies**: too little information for validation errors and conflicts.
