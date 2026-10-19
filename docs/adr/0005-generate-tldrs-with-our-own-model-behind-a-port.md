# 0005. Generate TL;DRs with our own model service, behind a port

- Status: Accepted
- Date: 2026-10-19

## Context

Every published post gets a short TL;DR. The original plan called a commercial LLM API through
Spring AI. The plan changed: the goal of this project includes **learning machine learning**, so
the model is built from scratch in [E-L-R/blog-tldr-model](https://github.com/E-L-R/blog-tldr-model):
a tokenizer, a GPT-style transformer, training and evaluation, in Python and PyTorch. Its own
decisions are recorded in that repository ([E-L-R/blog-tldr-model#6](https://github.com/E-L-R/blog-tldr-model/issues/6)).

That makes the model a separate service in another language, with its own release cycle. It is
also slower and less reliable than a hosted API: it can be busy, restarting, or misconfigured.
Publishing a post must never wait for it or fail because of it.

## Decision

- The `tldr` feature depends on a **port**: the `TldrGenerator` interface, with one method,
  `generate(TldrInput) → GeneratedTldr` ([#40](https://github.com/E-L-R/blog-backend/issues/40)). An **HTTP adapter** implements it against the model
  service. It uses an HTTP service interface with validated configuration and API-key authentication
  ([#39](https://github.com/E-L-R/blog-backend/issues/39)), prepares the input the model expects (Markdown to plain prose), validates the output, and
  classifies errors as transient or permanent.
- Generation is **asynchronous, after the commit** ([#41](https://github.com/E-L-R/blog-backend/issues/41)). Publishing a post (or changing its text)
  records a pending request inside the transaction. The model is called afterwards, outside any
  database transaction. The result is discarded if the post changed in the meantime. Unchanged input
  (same fingerprint) never costs a second call.
- **Resilience** ([#42](https://github.com/E-L-R/blog-backend/issues/42)): timeouts, targeted retries, backoff that honors `Retry-After`, a concurrency
  limit, leases so a job runs only once across instances, and a recovery job. A slow or down model
  delays TL;DRs, never posts.
- Each TL;DR records its **provenance** (which model version wrote it). Authors can write their own
  TL;DR instead ([#43](https://github.com/E-L-R/blog-backend/issues/43)).
- In production the model runs as a private sidecar container with no published ports and no route
  to the internet ([#53](https://github.com/E-L-R/blog-backend/issues/53)).

## Consequences

**Positive**

- Full control over the model, no per-token cost, and post content never leaves our infrastructure.
- The port keeps the rest of the code independent of the model: tests use a fake or WireMock, and a
  different model (or a hosted API) can be plugged in by writing another adapter.
- Publishing stays fast and reliable whatever the model does.

**Negative**

- One more service to build, deploy, monitor and secure ([#53](https://github.com/E-L-R/blog-backend/issues/53)).
- The summaries are less fluent than a frontier model's, and the quality depends on our own training
  and evaluation.
- Asynchronous generation adds states (`PENDING`, `FAILED`) that the API and every client must show,
  and a job system (leases, retries, recovery) that must be correct.

## Alternatives considered

- **Spring AI with a commercial LLM API** (the original plan, superseded before any code): the best
  summaries for the least code, but it teaches nothing about machine learning, costs money per token,
  and sends every post to a third party.
- **Running the model inside the JVM** (ONNX Runtime): no extra service, but it ties the model's
  release cycle and memory to the API, and it would mean exporting and serving a PyTorch model from Java.
- **Generating synchronously while publishing**: simpler, but publishing would be as slow and as
  fragile as the model.
