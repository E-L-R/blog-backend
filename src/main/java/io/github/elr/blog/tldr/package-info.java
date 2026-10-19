/**
 * TL;DRs: short summaries of published posts, generated in the background by our own model
 * service (E-L-R/blog-tldr-model), or written by the author.
 *
 * <p>The rest of the application depends on the {@code TldrGenerator} port, never on the HTTP client
 * that implements it (ADR-0005).
 */
package io.github.elr.blog.tldr;
