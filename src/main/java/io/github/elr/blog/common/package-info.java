/**
 * Cross-cutting building blocks with no knowledge of any feature: the JPA base entity, error
 * handling (problem details), paging and other web conventions, small text utilities.
 *
 * <p>Depends on nothing else in this application. Every feature may depend on it (enforced by
 * {@code ArchitectureTest}).
 */
package io.github.elr.blog.common;
