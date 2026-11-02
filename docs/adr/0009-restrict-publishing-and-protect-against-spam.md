# 0009. Restrict publishing to admins, and protect sign-ups and comments against spam

- Status: Accepted
- Date: 2026-11-02

## Context

The original plan let anyone who registers write and publish posts. The blog is public and indexed
by search engines, so that makes it a free place to host spam, and a target for automated accounts.
Every published post also starts a TL;DR generation on the server's CPU, so any account could keep
the model busy.

The blog has one writer. Readers are welcome to comment, but a comment box attracts spam too, and
moderation is one person's spare time. Subscriptions or payments would stop most of it, at the price
of a payment integration and of losing casual readers.

## Decision

**Writing**

- Only ADMIN accounts create, edit, publish or delete posts, and only they touch TL;DRs. The server
  enforces it with `@AdminOnly` on every such endpoint and answers 403 `forbidden` to everyone else.
  An ArchUnit rule requires the annotation, and an integration test proves it for every route, including
  that a USER never causes a model call
  ([#108](https://github.com/E-L-R/blog-backend/issues/108)). Hiding the editor in the frontend is a
  convenience, not a control.

**Registering**

- Registration is protected by Cloudflare Turnstile, verified on the server, before any other work. If
  Cloudflare can't be reached, registration fails closed with a 503. Nothing else depends on it.
- Registration answers one identical `202` for a new and for an already registered address, in about
  the same time. The owner of an existing address gets a notice by email, so the endpoint doesn't reveal
  who has an account here. Usernames stay a visible 409, because they're public anyway.

**Confirming an address**

- Commenting, reporting and editing a profile need a confirmed email address. The check reads the
  database, not the token, so it applies at once.
- A confirmation link is 256 random bits, stored as a SHA-256 hash, valid for 24 hours and for one use,
  and a new link replaces older ones. The token travels in the URL's fragment, and the page confirms
  only when someone presses a button, so logs don't see it and mail scanners can't use it up.
- A new link can be requested once a minute and 5 times a day, only by the signed-in account itself.
- Unconfirmed accounts can sign in, have no public profile, and are deleted after 7 days.

**Limiting the damage**

- Comments are plain text, limited to 5 per 10 minutes per user. Links in rendered Markdown get
  `rel="nofollow ugc"` from the sanitizer.
- Confirmed readers can report a comment, once each. Admins work through a queue.
- An admin can disable an account, which revokes its refresh tokens in the same transaction, and can
  erase its comments too.

**Email**

- Email goes through Resend's free plan (3,000 emails a month, 100 a day) over plain SMTP, behind an
  `EmailSender` port. Amazon SES is the fallback, and switching changes configuration, not code.

## Consequences

**Positive**

- Spam can't reach the posts at all, and the most expensive thing a stranger can trigger on the server
  is a bcrypt hash, behind a bot check at registration and a rate limit at login.
- Each fake commenter now costs an inbox and a solved challenge, and the worst one is one click away
  from being removed.
- People can't use the blog to find out whether someone has an account here.

**Negative**

- A reader has to confirm an address before their first comment, and some won't bother.
- Registration depends on Cloudflare and on an email provider being up.
- Resend's 100 emails a day can run out during a flood that gets past Turnstile. Links then wait until
  the next day, although accounts still work.
- A determined person with real inboxes can still post spam, one account at a time. Moderation stays
  a human job.
- `register` no longer returns a profile, and the frontend can't sign a new user in automatically.

## Alternatives considered

- **Keep open publishing and moderate afterwards**: spam would be public and indexed until someone
  noticed, and every post would cost model time.
- **Paid subscriptions**: strong against spam, but it adds a payment provider and tax questions, and
  it asks readers to pay to leave a comment.
- **Approve each comment by hand**: certain, but slow for readers, and it adds a daily chore.
- **reCAPTCHA or hCaptcha**: Turnstile is free without limits, and it doesn't track visitors across
  sites.
- **Rate limits alone**: they slow one source down, and they don't stop many accounts.
- **Amazon SES first**: it costs almost nothing, but a new account starts in a sandbox, and it would
  tie this feature to the AWS account from the start.
