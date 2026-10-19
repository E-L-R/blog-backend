# 0003. Organize code by feature, and enforce the rules with ArchUnit

- Status: Accepted
- Date: 2026-10-19

## Context

The codebase will grow to six features (authentication, users, posts, tags, comments, TL;DRs), each
with controllers, services, repositories, entities and DTOs. Spring Boot doesn't prescribe a layout.

The common default, **package-by-layer** (`controllers/`, `services/`, `repositories/`), scatters one
feature over many packages. A change to comments touches five directories, everything has to be
`public`, and nothing stops one feature from reaching into another's internals.

Layering rules also erode quietly: one controller injecting a repository "just this once" passes
code review, and a few months later the layering is gone.

## Decision

Code is organized **by feature** under `io.github.elr.blog`:

- **Features**: `auth`, `user`, `post`, `tag`, `comment`, `tldr`. Each one holds its own controller,
  service, repository, entities and DTOs.
- **Shared infrastructure**: `common` (errors, web conventions, persistence base classes), `config`
  and `security`.
- Each package has a `package-info.java` stating its responsibility.
- Package-private is the default visibility. A class is `public` only if another package needs it.

The rules are **executable**: an ArchUnit `ArchitectureTest` ([#10](https://github.com/E-L-R/blog-backend/issues/10)) fails the build when they are
broken. For example: no dependency cycles between features, `common` doesn't depend on features,
controllers don't use repositories, entities don't leak out of controllers, and only Spring's
`@Transactional` is used.

## Consequences

**Positive**

- Code that changes together lives together. A feature can be understood, reviewed or deleted as a unit.
- Package-private visibility hides a feature's internals from the others.
- Architecture violations fail CI like any other test, instead of relying on reviewers noticing them.
- The features are natural module boundaries if we adopt Spring Modulith (or split out a service) later.

**Negative**

- Deciding where cross-cutting code belongs (`common`, or the feature that owns it) takes judgment.
- Some features depend on each other (comments on posts, TL;DRs on posts), so the "no cycles" rule
  sometimes forces an extra interface or event instead of a direct call.
- ArchUnit rules are code to maintain, and a rule that matches nothing has to be marked explicitly
  (`allowEmptyShould`) until the classes it targets exist.

## Alternatives considered

- **Package-by-layer**: familiar from tutorials, but it optimizes for the technical role of a class
  instead of the change you are making, and it forces everything to be `public`.
- **Hexagonal architecture everywhere** (ports and adapters for every feature): clean, but heavy for
  CRUD features. We use a port only where an external system sits behind it: the TL;DR model
  ([ADR-0005](0005-generate-tldrs-with-our-own-model-behind-a-port.md)).
- **Spring Modulith now**: module verification plus event publication for this exact structure. A
  good next step once the modules exist; ArchUnit first makes the rules explicit.
- **Separate Maven modules per feature**: compile-time boundaries, but more build configuration than
  a single small service needs.
