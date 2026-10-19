package io.github.elr.blog.common.error;

import java.net.URI;

/**
 * Stable, documented problem "type" URIs (RFC 9457 §3.1.1). Clients branch on {@code type}, never on
 * the human-readable {@code title}/{@code detail}. Each URI points at its explanation in docs/problems.md.
 */
public final class ProblemTypes {

    private static final String BASE = "https://github.com/E-L-R/blog-backend/blob/main/docs/problems.md#";

    public static final URI VALIDATION = URI.create(BASE + "validation");
    public static final URI BAD_REQUEST = URI.create(BASE + "bad-request");
    public static final URI UNAUTHORIZED = URI.create(BASE + "unauthorized");
    public static final URI FORBIDDEN = URI.create(BASE + "forbidden");
    public static final URI NOT_FOUND = URI.create(BASE + "not-found");
    public static final URI CONFLICT = URI.create(BASE + "conflict");
    public static final URI RATE_LIMITED = URI.create(BASE + "rate-limited");
    public static final URI INTERNAL = URI.create(BASE + "internal");

    private ProblemTypes() {}
}
