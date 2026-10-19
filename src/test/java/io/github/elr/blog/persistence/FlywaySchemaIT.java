package io.github.elr.blog.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.elr.blog.testsupport.BaseIntegrationTest;
import io.github.elr.blog.testsupport.DatabaseCleaner;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

/** The schema's constraints are a second line of defense behind validation: test them directly. */
class FlywaySchemaIT extends BaseIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    Flyway flyway;

    @BeforeEach
    void clean() {
        new DatabaseCleaner(jdbc).truncateAll();
    }

    @Test
    void baseline_migration_is_applied() {
        assertThat(flyway.info().applied())
                .anySatisfy(m -> assertThat(m.getVersion().getVersion()).isEqualTo("1"));
    }

    @Test
    void usernames_are_unique_ignoring_case() {
        insertUser("Alice", "alice@example.com");
        assertThatThrownBy(() -> insertUser("alice", "other@example.com")).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void emails_are_unique_ignoring_case() {
        insertUser("alice", "Alice@Example.com");
        assertThatThrownBy(() -> insertUser("bob", "alice@example.com")).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void username_format_is_enforced_by_the_database() {
        assertThatThrownBy(() -> insertUser("no spaces!", "x@example.com"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void a_published_post_needs_a_publication_date() {
        UUID author = insertUser("author", "author@example.com");
        assertThatThrownBy(() -> jdbc.update("""
                        INSERT INTO posts (id, author_id, title, slug, content_markdown, status, published_at, created_at, updated_at)
                        VALUES (?, ?, 'T', 'slug', 'body', 'PUBLISHED', NULL, now(), now())
                        """, UUID.randomUUID(), author))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void slugs_are_unique() {
        UUID author = insertUser("author", "author@example.com");
        insertDraft(author, "same-slug");
        assertThatThrownBy(() -> insertDraft(author, "same-slug")).isInstanceOf(DuplicateKeyException.class);
    }

    private UUID insertUser(String username, String email) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO users (id, username, email, password_hash, display_name, created_at, updated_at)
                VALUES (?, ?, ?, 'hash', 'Name', now(), now())
                """, id, username, email);
        return id;
    }

    private void insertDraft(UUID author, String slug) {
        jdbc.update("""
                INSERT INTO posts (id, author_id, title, slug, content_markdown, created_at, updated_at)
                VALUES (?, ?, 'T', ?, 'body', now(), now())
                """, UUID.randomUUID(), author, slug);
    }
}
