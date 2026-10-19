# 0006. Store data in PostgreSQL, with the schema owned by Flyway migrations

- Status: Accepted
- Date: 2026-10-19

## Context

The data is relational: users write posts, posts have tags and comments, and refresh tokens belong
to users. Correctness matters more than flexibility: usernames must be unique regardless of case, a
published post must have a publication date, and a token hash must never appear twice.

The schema will change with almost every feature. Those changes must be reviewable, repeatable on
every environment, and never silently applied by the ORM. Tests should catch problems that only a
real database shows: constraint violations, query plans, full-text search.

## Decision

- **PostgreSQL 17** is the only database, in development (`compose.yaml`), in tests and in production.
- **Flyway owns the schema**: versioned SQL migrations in `src/main/resources/db/migration`
  (`V1__init.sql`, …), reviewed like code ([#11](https://github.com/E-L-R/blog-backend/issues/11)). A migration that has been merged is never edited;
  changes go into a new migration.
- Hibernate only checks the mapping: `spring.jpa.hibernate.ddl-auto=validate`, never `update`.
- Rules live in the database as well as in the code: `NOT NULL`, `CHECK`, unique and foreign-key
  constraints, plus indexes for real access patterns, including partial and expression indexes
  (for example a unique index on `lower(username)` and an index on published posts only).
- Tests run against real PostgreSQL through **Testcontainers** ([#4](https://github.com/E-L-R/blog-backend/issues/4)), with the same image as
  `compose.yaml`, which `PostgresImageConsistencyTest` enforces. No H2.

## Consequences

**Positive**

- Every schema change is a reviewed, versioned file, applied the same way everywhere. Flyway records
  what ran, and `validate` fails startup if the code and the schema disagree.
- Database constraints protect the data even from bugs and from concurrent requests that slip past
  the application's checks.
- Tests exercise the real SQL dialect, constraints and features (full-text search, partial indexes).

**Negative**

- Migrations must be written by hand in SQL, and every change needs a new migration, even a small one.
- Rolling back means writing a forward migration. Production releases must keep migrations compatible
  with the previous app version ([#55](https://github.com/E-L-R/blog-backend/issues/55)).
- Tests need Docker and are slower than an in-memory database.

## Alternatives considered

- **`ddl-auto=update`**: no SQL to write, but it applies unreviewed changes, never drops or renames
  anything safely, and differs between environments.
- **Liquibase**: equivalent capabilities with XML/YAML changelogs; plain SQL with Flyway is easier to
  read and review.
- **H2 in tests**: fast, but it's a different database with a different dialect and different
  constraint behavior, so tests can pass while production fails.
- **A document database** (MongoDB): flexible, but the data is relational and the integrity rules
  would move into application code.
