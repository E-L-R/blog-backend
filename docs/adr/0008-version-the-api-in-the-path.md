# 0008. Version the API in the path, with Spring Framework 7's API versioning

- Status: Accepted
- Date: 2026-10-19

## Context

Once clients exist (the React frontend, scripts, the HTTP collection), breaking changes to the API
must not break them. A version must be visible from the first endpoint on: adding it later would
itself be a breaking change.

Spring Framework 7 has first-class API versioning. A resolver reads the version from the request (a
path segment, a header, a query parameter or the media type), and request mappings declare the
version they serve.

## Decision

- The version is a **path segment**: `/api/v1/...`.
- Controllers map `/api/{version}/...` and declare `version = "v1"` ([#15](https://github.com/E-L-R/blog-backend/issues/15)). The string is `"v1"`, not
  `"1"`, so springdoc documents the paths as `/api/v1/...`. The versioning configuration
  (`ApiVersioningConfig`) applies to `/api/**` only.
- An unsupported version (`/api/v2/...`) is a **400** problem detail
  ([ADR-0007](0007-report-errors-as-rfc-9457-problem-details.md)).
- Only breaking changes get a new version. Then `version = "v2"` mappings are added next to the v1
  ones, v1 keeps working through an announced deprecation period, and is removed afterwards.
  Additive changes (new fields, new endpoints) stay in v1.

## Consequences

**Positive**

- The version is visible in every URL, log line, curl command and browser address bar, and easy to
  route and cache.
- Spring handles the parsing, validation and routing. The controllers stay ordinary mappings.
- v1 and v2 can run side by side in one application during a migration.

**Negative**

- The version is part of every resource's URL, so `/api/v1/posts/x` and `/api/v2/posts/x` name the
  same resource twice, which purists object to.
- Every controller must remember `version = "v1"` (a rule that tests and review must catch), and
  clients must change their base URL to move to a new version.

## Alternatives considered

- **A header** (`API-Version: 1`): clean URLs, but invisible in links and logs, and impossible to try
  in a browser without tooling.
- **Media-type versioning** (`Accept: application/vnd.blog.v1+json`): the most "RESTful", and the
  least discoverable. Every client has to set custom media types.
- **No versioning, only additive changes**: works until the first unavoidable breaking change, and
  then there's no clean way to keep old clients working.
