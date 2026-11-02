# 0010. Run on one EC2 Spot instance behind Cloudflare Tunnel, with kill switches

- Status: Accepted
- Date: 2026-11-02

## Context

The backend, the TL;DR model, PostgreSQL and a reverse proxy have to run somewhere. The frontend stays
on Netlify. The target is a monthly AWS bill of at most 30 USD, and being secure and resilient to
denial-of-service attacks at that price. The author lives in South Korea and has no AWS account yet.

The stack needs about 4 GB of memory, and generation is CPU-bound. Model training never runs here.

Prices checked on 2026-11-02, for 730 hours in Seoul (`ap-northeast-2`):

| Option | USD a month |
| --- | --- |
| On-demand `t4g.small` (2 GB, too small) | 15.18 |
| On-demand `t4g.medium` (4 GB) | 30.37 |
| On-demand `m6g.medium` (4 GB) | 34.31 |
| Spot `m6g.medium`, about 74% below on-demand | about 9 |

On top of that come 3.65 USD for a public IPv4 address and 0.0912 USD per GB-month for gp3 storage.
The first 100 GB of outbound traffic each month are free, then it's 0.126 USD/GB.

AWS Budgets data can lag by most of a day, and no AWS setting guarantees a limit across all services.

## Decision

**Compute**

- One persistent Spot `m6g.medium` (Arm, 1 vCPU, 4 GB) in Seoul, with a maximum price of 0.02 USD an
  hour. When AWS interrupts it, it stops and keeps its disk.
- Docker Compose runs `cloudflared`, Caddy, the app, PostgreSQL and the model on that instance. The
  images are built for `linux/arm64` on GitHub's free Arm runners.
- Container limits keep the model from starving the API, and the model has a bounded queue.
- No GPU, ECS, RDS, SageMaker, NAT gateway or load balancer.

**Network**

- Cloudflare's free plan sits in front of the API. Cloudflare Tunnel is the only way in, so the
  security group has no inbound rules, and the origin has no port to attack.
- Caddy takes the client's address only from `CF-Connecting-IP`, so rate limits can't be fooled by
  forwarded headers.
- Defenses are layered: Cloudflare, Caddy, Tomcat's connection limit, request size limits, application
  rate limits, the connection pool and a statement timeout.
- No AWS WAF or Shield Advanced. AWS Shield Standard is included.

**Access and secrets**

- Systems Manager replaces SSH from outside: sshd listens on localhost only. IMDSv2 is required, with a
  hop limit of 1, and the disk is encrypted.
- GitHub Actions signs in with OIDC, and only the `production` environment can. Its role can run exactly
  one Systems Manager document, which deploys images by digest.
- Secrets live in Parameter Store as SecureString parameters, which are free.
- Terraform manages AWS, in `infra/`. Cloudflare is configured by hand, because it's a handful of
  settings.

**Backups**

- Encrypted nightly dumps go to S3 in Tokyo, with public access blocked, Object Lock and a lifecycle
  rule. The server can add backups, and it can't read or delete them.

**Billing protection**

| Level | Trigger | What happens |
| --- | --- | --- |
| Alerts | 10, 20 and 25 USD actual; 25 USD forecast; anomalies | email |
| Traffic breaker | more than 1 GiB out in 5 minutes, or 10 GiB in a day | the server stops |
| Budget stop | 25 USD actual | the server stops |
| Spend limit | 30 USD, if the account offers it | AWS pauses EC2 |

The stop is set at 25 USD, not 30, to leave room for billing lag. A stopped server keeps costing about
2 USD a month, and one command brings it back.

## Consequences

**Positive**

- The estimated bill is about 14.50 USD a month, and about 20 USD at the Spot price cap, with a domain
  on top.
- With no open ports, a whole class of attacks disappears, and Cloudflare absorbs floods for free.
- The whole server can be rebuilt from this repository, Parameter Store and a backup.

**Negative**

- One instance is a single point of failure. An interruption or a kill switch means downtime, not data
  loss.
- A stopped Spot instance restarts only in its own Availability Zone, and only when there's capacity.
- One vCPU makes summaries slow, and they share it with everything else.
- Cloudflare is a hard dependency: if it's down, the API is down.
- Backups run nightly, so up to 24 hours of data can be lost.
- Billing remains a soft limit. Stolen AWS credentials could start resources these switches don't cover
  before anyone reacts, which is why the account relies on MFA and has no long-lived keys. A flood from
  many addresses is limited only by Cloudflare's automatic protection and one free rate-limiting rule.
- The kill switches can't be proven without an account, so drills are part of building them
  ([#111](https://github.com/E-L-R/blog-backend/issues/111)).

## When to revisit this

- **Downtime starts to matter**, for example the blog earns money, or interruptions or capacity gaps
  add up to more than about a day a month: move to on-demand, and raise the budget.
- **24 hours of possible data loss is too much**: add WAL archiving or a managed database.
- **Summaries get too slow, or the model needs a GPU**: run the model somewhere else, behind the same
  port (ADR-0005).
- **One instance can't carry the traffic**: rate limits and sessions then need shared storage.
- **The Spot price stays above the 0.02 USD cap**: the budget no longer holds, so choose again.

## Alternatives considered

- **On-demand instead of Spot**: simpler and steadier, but 34.31 USD a month for `m6g.medium` alone
  can't fit under a 25 USD kill switch.
- **Savings Plans**: billed even while stopped, which defeats the kill switch.
- **`t4g.small`**: 2 GB can't hold the stack. Burstable types also bill or throttle sustained CPU.
- **Oregon**: cheaper on-demand, but far from the author and readers in Korea, and Spot makes the
  difference small.
- **ECS, RDS, SageMaker or a load balancer**: each costs more per month than this whole setup.
- **Cloudflare's IP ranges in the security group**: it works, but it leaves a port open, and the list
  has to be kept in sync.
- **Secrets Manager**: charged per secret, for rotation this project doesn't use.
- **Manual setup or CDK**: manual setup can't be rebuilt reliably, and Terraform stays simple here.
