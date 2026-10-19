package io.github.elr.blog.testsupport;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class DatabaseCleanerIT extends BaseIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void truncate_all_is_safe_on_any_schema() {
        assertThatCode(() -> new DatabaseCleaner(jdbc).truncateAll()).doesNotThrowAnyException();
    }
}
