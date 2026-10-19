/**
 * The HTTP security layer: the stateless filter chain, JWT encoding and decoding, and
 * authorization helpers.
 *
 * <p>Endpoints that sign users in, out or up belong to {@code auth}; this package only decides who a
 * request comes from and what it may do.
 */
package io.github.elr.blog.security;
