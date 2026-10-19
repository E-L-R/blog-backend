# Problem types

Every error response is `application/problem+json` ([RFC 9457](https://www.rfc-editor.org/rfc/rfc9457)).
**Branch on `type`, never on `title` or `detail`**: `type` is stable, while `detail` is a human-readable
explanation whose wording may change. Each `type` is this page's URL plus the anchor of a section
below, for example `https://github.com/E-L-R/blog-backend/blob/main/docs/problems.md#conflict`.

```json
{
  "type": "https://github.com/E-L-R/blog-backend/blob/main/docs/problems.md#validation",
  "title": "Validation failed",
  "status": 400,
  "detail": "One or more fields are invalid.",
  "instance": "/api/v1/auth/register",
  "errors": [{ "field": "email", "message": "must be a well-formed email address" }]
}
```

| Member | Meaning |
| --- | --- |
| `type` | The kind of problem: one of the sections below (absent for [Spring MVC's own errors](#errors-without-one-of-these-types)) |
| `title` | A short summary of the type |
| `status` | The HTTP status code, repeated in the body |
| `detail` | What went wrong in this request, for humans |
| `instance` | The request path |
| `errors` | Only for [`validation`](#validation): one `{field, message}` per invalid field |

Every response, errors included, has an `X-Trace-Id` header. Quote it when reporting a problem: it
finds the request in the server logs.

Why this format: [ADR-0007](adr/0007-report-errors-as-rfc-9457-problem-details.md).

## validation

**400 Bad Request.** One or more request fields or parameters are invalid. `errors[]` lists a
`{field, message}` for each one, so clients can show each message next to its input. Fix the input
and send the request again.

## bad-request

**400 Bad Request.** The request is well-formed but can't be carried out as asked, for example
sorting by a field that isn't allowed, or a wrong current password when changing it. Sending it again
unchanged won't help.

## unauthorized

**401 Unauthorized.** The access token is missing, expired or invalid, or the credentials are wrong.
Refresh the access token and retry once. If that fails too, sign in again.

## forbidden

**403 Forbidden.** You are authenticated but not allowed to do this, for example editing someone
else's post. Retrying won't help.

## not-found

**404 Not Found.** The resource doesn't exist, or isn't visible to you: unpublished drafts are 404
for everyone except their author, so their existence doesn't leak.

## conflict

**409 Conflict.** The request conflicts with the current state: a username or email that is already
taken, or a resource that someone else modified since you loaded it. For the latter, reload the
resource and apply your change again.

## rate-limited

**429 Too Many Requests.** Too many requests in a short time. Wait for the number of seconds in the
`Retry-After` header before trying again.

## internal

**500 Internal Server Error.** An unexpected error on the server. The response never contains
internal details. They are logged on the server under the request's `X-Trace-Id`. Retrying later may
help, and reporting the trace id helps more.

## Errors without one of these types

Some errors produced by Spring MVC itself, such as an unknown URL (404), a wrong HTTP method (405),
an unsupported `Content-Type` (415), malformed JSON or an unsupported API version (400), are problem
details too, but they have **no `type` member**. RFC 9457 says a missing `type` means `about:blank`:
nothing more to say than the status code. Treat them by their `status`:

```json
{
  "title": "Method Not Allowed",
  "status": 405,
  "detail": "Method 'PUT' is not supported.",
  "instance": "/api/v1/auth/login"
}
```
