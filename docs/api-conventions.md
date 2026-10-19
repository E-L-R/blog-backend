# API conventions

How every endpoint looks, so the API feels like one design. [#15](https://github.com/E-L-R/blog-backend/issues/15)
adds versioning, status codes, `Location` headers, paging and sorting to this page.

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
- **Never expose entities**: controllers return DTOs ([ADR-0003](adr/0003-organize-code-by-feature.md),
  enforced by `ArchitectureTest`), so the documented models are the API, not the database schema.
- **Test-only controllers** are `@TestComponent`
  ([#13](https://github.com/E-L-R/blog-backend/issues/13)), so they never appear in the document.
