# Documentation

Everything about how blog-backend is designed, used and operated. Start with the
[project README](../README.md) for an overview and local development.

## Architecture decision records

Why the system is built the way it is: one decision per record, never edited after acceptance
([ADR-0001](adr/0001-record-architecture-decisions.md)). New decisions start from the
[template](adr/0000-template.md).

| ADR | Decision | Status |
| --- | --- | --- |
| [0001](adr/0001-record-architecture-decisions.md) | Record architecture decisions | Accepted |
| [0002](adr/0002-serve-a-rest-only-json-api.md) | Serve a REST-only JSON API | Accepted |
| [0003](adr/0003-organize-code-by-feature.md) | Organize code by feature, and enforce the rules with ArchUnit | Accepted |
| [0004](adr/0004-jwt-access-tokens-with-rotating-refresh-tokens.md) | Authenticate with short-lived JWT access tokens and rotating refresh tokens | Accepted |
| [0005](adr/0005-generate-tldrs-with-our-own-model-behind-a-port.md) | Generate TL;DRs with our own model service, behind a port | Accepted |
| [0006](adr/0006-store-data-in-postgresql-with-flyway-migrations.md) | Store data in PostgreSQL, with the schema owned by Flyway migrations | Accepted |
| [0007](adr/0007-report-errors-as-rfc-9457-problem-details.md) | Report every error as an RFC 9457 problem detail | Accepted |
| [0008](adr/0008-version-the-api-in-the-path.md) | Version the API in the path, with Spring Framework 7's API versioning | Accepted |

## Reference

- [Problem types](problems.md): every error `type` the API returns, and what a client should do about it.
- [Database schema](er-diagram.md): the ER diagram, the rules the database enforces, and the indexes.
- [API conventions](api-conventions.md): JSON, status codes, paging and sorting, versioning, and how endpoints are documented (Swagger UI at `/swagger-ui.html`).

## Coming with the features

Each of these documents is added by the issue that builds the feature:

| Document | Issue |
| --- | --- |
| Security design: the filter chain, JWTs, registration, login, refresh-token rotation | [#17](https://github.com/E-L-R/blog-backend/issues/17)–[#21](https://github.com/E-L-R/blog-backend/issues/21) |
| Deployment: production Compose stack, the VM and continuous deployment, the Netlify frontend's API host | [#53](https://github.com/E-L-R/blog-backend/issues/53), [#81](https://github.com/E-L-R/blog-backend/issues/81), [#86](https://github.com/E-L-R/blog-backend/issues/86) |
| Runbooks: backups and the restore drill | [#82](https://github.com/E-L-R/blog-backend/issues/82) |
| The OpenAPI document (`openapi.json`), checked by a test, and the final architecture diagrams | [#57](https://github.com/E-L-R/blog-backend/issues/57) |
