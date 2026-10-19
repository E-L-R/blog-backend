# 0002. Serve a REST-only JSON API

- Status: Accepted
- Date: 2026-10-19

## Context

The blog needs a reading experience, an editor for authors, and moderation. Spring Boot could render
those pages on the server (Thymeleaf), or the backend could expose only data and leave the UI to
separate clients.

The project's goal is a production-style backend: a clear HTTP contract, security for token-based
clients, and tests at the API boundary. The UI is a separate concern, and it now exists as its own
project: [E-L-R/blog-frontend](https://github.com/E-L-R/blog-frontend), a React single-page app on
Netlify. Tools like curl, Postman and the HTTP collection ([#56](https://github.com/E-L-R/blog-backend/issues/56)) are clients too.

## Decision

The backend is a **REST API that speaks JSON only**. It renders no HTML pages.

- Resources live under `/api/v1/...`, versioned by path ([#15](https://github.com/E-L-R/blog-backend/issues/15), [ADR-0008](0008-version-the-api-in-the-path.md)).
- Every error is an RFC 9457 problem detail ([ADR-0007](0007-report-errors-as-rfc-9457-problem-details.md)).
- The contract is published as an OpenAPI document with Swagger UI ([#14](https://github.com/E-L-R/blog-backend/issues/14)), and clients are built against it.
- Browsers reach the API from another origin, so CORS allows exactly the frontend's origins ([#47](https://github.com/E-L-R/blog-backend/issues/47), [#86](https://github.com/E-L-R/blog-backend/issues/86)).

## Consequences

**Positive**

- Any client can be built, replaced or added without touching the backend: the React frontend, a
  mobile app, scripts.
- The HTTP contract is the whole interface, so it can be tested directly (MockMvc and integration
  tests) and documented in one place.
- The backend scales and deploys independently of the UI (static hosting for the frontend, [#86](https://github.com/E-L-R/blog-backend/issues/86)).

**Negative**

- Two projects to build, deploy and keep in sync. Contract changes must be coordinated, and the
  frontend keeps a copy of the OpenAPI document that it tests against ([#89](https://github.com/E-L-R/blog-backend/issues/89)).
- Cross-origin requests need CORS, and browser clients must store tokens themselves
  ([ADR-0004](0004-jwt-access-tokens-with-rotating-refresh-tokens.md)).
- No server-rendered HTML for search engines: the frontend has to handle SEO (pre-rendering or meta
  tags) itself.

## Alternatives considered

- **Server-side rendering with Thymeleaf** (plus a few JSON endpoints): one deployable and simple SEO,
  but it couples UI and API in one codebase and teaches little about building a public API.
- **GraphQL**: flexible queries for clients, but caching, rate limiting and authorization per field
  are harder, and REST with pagination is enough for a blog's access patterns.
- **gRPC**: efficient between services, but browsers can't call it directly.
