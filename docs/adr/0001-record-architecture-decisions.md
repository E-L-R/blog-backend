# 0001. Record architecture decisions

- Status: Accepted
- Date: 2026-10-19

## Context

This API is built issue by issue, with design choices made weeks before the code that depends on
them. Without a record, the reasons behind those choices get lost. Months later, nobody can tell
whether a choice still holds or was an accident, and reviewers (and interviewers) keep asking the
same question: *why is it built this way?*

Commit messages and pull requests describe *changes*, not the decisions behind them, and they are
hard to find again. A wiki drifts away from the code.

## Decision

We record every architecturally significant decision as an Architecture Decision Record (ADR), as
described by Michael Nygard:

- One decision per file in `docs/adr/`, named `NNNN-title-in-the-imperative.md` and numbered in order.
- Every ADR follows [`0000-template.md`](0000-template.md): Status, Date, Context, Decision,
  Consequences (positive *and* negative), Alternatives considered.
- An ADR is written **when the decision is made**, in the same pull request as the code it shapes
  (or before it), and reviewed like code.
- **Accepted ADRs are immutable.** When a decision changes, a new ADR replaces it, and the old one
  only gets its status changed to `Superseded by NNNN`. Typos and broken links may be fixed.
- "Architecturally significant" means: expensive to reverse, affects several features, or a reader
  of the code would wonder why.

## Consequences

**Positive**

- The reasoning, including the options we rejected, lives next to the code and is versioned with it.
- New contributors (and future-us) can read the history of the architecture in order.
- Writing the alternatives down before coding forces a real comparison while changing course is still cheap.

**Negative**

- Every significant change takes a little more effort: someone has to write the ADR.
- The record is only as good as our discipline. A decision made without an ADR is invisible.

## Alternatives considered

- **No written record**: free, but the reasons vanish with the people (or the memory) that held them.
- **A wiki page or one big design document**: easy to edit, but it lives outside the repository,
  isn't reviewed with the code, and gets rewritten in place, so the history of *why* is lost.
- **MADR** (Markdown Any Decision Records): a richer template with decision drivers and pros/cons per
  option. Worth adopting if decisions grow more complex; Nygard's format is enough for now.
