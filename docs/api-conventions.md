# API conventions

How every endpoint looks, so the API feels like one design
([#14](https://github.com/E-L-R/blog-backend/issues/14), [#15](https://github.com/E-L-R/blog-backend/issues/15)).

## Base path and JSON

- Every endpoint lives under **`/api/v1`**, for example `GET /api/v1/posts`. See [versioning](#versioning).
- Requests and responses are JSON (`application/json`), errors are `application/problem+json`.
- Property names are **camelCase** (`publishedAt`, `displayName`).
- Timestamps are **ISO-8601 in UTC**: `"2026-10-14T12:00:00Z"`.
- Ids are **UUIDs as strings**: `"0199f3b1-5c2e-7a3b-9c4d-1e2f3a4b5c6d"`.
- Missing values are `null`, not left out: `"bio": null`.

## Status codes

| Status | When | Example |
| --- | --- | --- |
| `200 OK` | Reads and updates, with the resource in the body | `GET /api/v1/posts/hello-world` |
| `201 Created` | Something was created. The `Location` header has the new resource's absolute URL, and the body has the resource. | `POST /api/v1/posts` → `Location: https://…/api/v1/users/me/posts/hello-world` (a draft is only visible to its author, so the URL points at the author's view) |
| `202 Accepted` | The work is queued, not done: the body shows the current state | `POST /api/v1/posts/hello-world/tldr/regenerate` ([#43](https://github.com/E-L-R/blog-backend/issues/43)) |
| `204 No Content` | Deletes, and other successful requests with nothing to return | `DELETE /api/v1/posts/hello-world` |
| `4xx` / `5xx` | Errors, always as problem details | See [problem types](problems.md) |

## Errors

Every error is an RFC 9457 problem detail. Clients branch on its `type`, which is documented in
[`problems.md`](problems.md), and never on `title` or `detail`
([ADR-0007](adr/0007-report-errors-as-rfc-9457-problem-details.md)).

## Paging and sorting

List endpoints take **`page`** (zero-based, default `0`), **`size`** (default `20`, **at most 50**)
and **`sort`** (`property,asc|desc`, repeatable):

```http
GET /api/v1/posts?page=0&size=20&sort=publishedAt,desc
```

They return a **`PageResponse`**, never Spring Data's `Page`, whose JSON shape is an implementation
detail that changes between versions:

```json
{
  "content": [{ "slug": "hello-world", "title": "Hello, world" }],
  "page": 0,
  "size": 20,
  "totalElements": 42,
  "totalPages": 3
}
```

**Each endpoint has an allow-list of sortable properties and a default sort**, and passes the
client's `Pageable` through `PageRequests.sanitize(...)`:

```java
@GetMapping
PageResponse<PostSummary> list(@ParameterObject Pageable pageable) {
    Pageable safe = PageRequests.sanitize(pageable, Set.of("publishedAt", "title"), Sort.by(DESC, "publishedAt"));
    return PageResponse.from(postService.listPublished(safe), PostSummary::from);
}
```

- Sorting by a property that isn't allowed is a `400` [`bad-request`](problems.md#bad-request) that
  names the allowed ones: `Cannot sort by 'passwordHash'. Allowed: publishedAt, title`. Allowing
  every property would let clients probe internal fields, or sort 100 KB text columns that have no
  index on every request.
- A `size` above 50 is quietly capped at 50.
- No `sort` parameter means the endpoint's default sort.
- `@ParameterObject` makes the OpenAPI document show `page`, `size` and `sort` as query parameters.
  Without it, the document claims a single required `pageable` parameter.

## Versioning

The version is the second path segment: `/api/v1/...`. Spring Framework 7's API versioning
([ADR-0008](adr/0008-version-the-api-in-the-path.md)) is set up in
[`ApiVersioningConfig`](../src/main/java/io/github/elr/blog/common/web/ApiVersioningConfig.java).
Every controller declares the version once:

```java
@RestController
@RequestMapping(path = "/api/{version}/posts", version = "v1")
class PostController { ... }
```

- Write **`version = "v1"`, not `"1"`**. Both route, but the OpenAPI document substitutes the declared
  string, so `"1"` would document `/api/1/posts` while clients call `/api/v1/posts`.
- An unsupported version (`/api/v2/posts`) is a `400` problem detail.
- Only `/api/**` is versioned. Actuator, the OpenAPI document and Swagger UI need no version.
- A breaking change gets a `version = "v2"` mapping next to the v1 one. v1 keeps working through an
  announced deprecation period, then goes. Additive changes, like new fields or new endpoints, stay in v1.

## Documentation (OpenAPI)

The API documents itself. springdoc-openapi reads the controllers, the request and response records
and their validation annotations at runtime, and generates an OpenAPI 3.1 document
([#14](https://github.com/E-L-R/blog-backend/issues/14)):

| What | Where |
| --- | --- |
| OpenAPI document (JSON) | `/v3/api-docs` |
| Swagger UI | `/swagger-ui.html` |
| Metadata, version and security scheme | [`OpenApiConfig`](../src/main/java/io/github/elr/blog/config/OpenApiConfig.java) |

The version shown is the build's (from the POM, through Spring Boot's `build-info`). Runs that
skip that Maven goal, such as some IDE runs, show `dev` instead.

**The docs are off in production** (the `prod` profile) unless `BLOG_API_DOCS_ENABLED=true`, for
example on a staging server. They map every endpoint, parameter and model, and production doesn't
need to serve that.

### Documenting an endpoint

The defaults cover most of it. Add annotations where they say something the code doesn't:

```java
@Tag(name = "Posts")                                                // one tag per feature
@RestController
@RequestMapping(path = "/api/{version}/posts", version = "v1")     // "v1", not "1" (#15)
class PostController {

    @Operation(summary = "Create a draft post")                     // a short imperative summary
    @SecurityRequirement(name = OpenApiConfig.BEARER)               // only on endpoints that need a token
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(
            responseCode = "400",
            description = "Validation failed",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    ResponseEntity<PostDetail> create(@Valid @RequestBody CreatePostRequest request) { ... }
}
```

- **`@Tag`**: one per feature (`Auth`, `Users`, `Posts`, `Tags`, `Comments`, `TL;DR`), so Swagger UI
  groups operations the way the code is organized.
- **`@Operation(summary = ...)`**: a short imperative phrase ("Publish a post"). Add a `description`
  only for behavior a reader can't guess, such as side effects or the TL;DR being generated afterwards.
- **`@SecurityRequirement(name = OpenApiConfig.BEARER)`**: on operations that need an access token,
  and only there. Most of the API is public (reading posts, tags and comments), so the scheme is
  never declared globally. Swagger UI shows a lock on these operations, and its **Authorize** button
  takes a token from `POST /api/v1/auth/login`.
- **`@ApiResponse`**: list the success status (`201`, `204`, ...) and the errors a client should
  expect from *this* operation. Errors are always `application/problem+json` with
  `ProblemDetail` as the schema ([problem types](problems.md)).
- **Request and response bodies are records** with Jakarta Validation annotations (`@NotBlank`,
  `@Size(max = 200)`, ...). springdoc turns them into schema constraints, so documentation and
  validation can't drift apart. Add `@Schema(description = ..., example = ...)` where a field's
  meaning or format isn't obvious.
- **List endpoints take `@ParameterObject Pageable`**, so paging is documented as `page`, `size` and
  `sort` (see [paging](#paging-and-sorting)).
- **Never expose entities**: controllers return DTOs ([ADR-0003](adr/0003-organize-code-by-feature.md),
  enforced by `ArchitectureTest`), so the documented models are the API, not the database schema.
- **Test-only controllers** are `@TestComponent`
  ([#13](https://github.com/E-L-R/blog-backend/issues/13)), so they never appear in the document.
