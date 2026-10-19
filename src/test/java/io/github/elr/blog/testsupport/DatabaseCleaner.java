package io.github.elr.blog.testsupport;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Empties every application table (keeps Flyway's history table) so a test starts from a known state.
 *
 * <p>Uses TRUNCATE ... CASCADE, which is much faster than DELETE and resets all foreign-key-related
 * tables in one statement.
 */
public final class DatabaseCleaner {

    private final JdbcTemplate jdbc;

    public DatabaseCleaner(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void truncateAll() {
        List<String> tables = jdbc.queryForList("""
            SELECT quote_ident(table_name)
            FROM information_schema.tables
            WHERE table_schema = 'public'
              AND table_type = 'BASE TABLE'
              AND table_name <> 'flyway_schema_history'
            """, String.class);
        if (tables.isEmpty()) {
            return;
        }
        jdbc.execute("TRUNCATE TABLE " + tables.stream().collect(Collectors.joining(", ")) + " CASCADE");
    }
}
