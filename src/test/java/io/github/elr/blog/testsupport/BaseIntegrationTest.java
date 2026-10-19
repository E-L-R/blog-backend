package io.github.elr.blog.testsupport;

import io.github.elr.blog.TestcontainersConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for integration tests that need the full application context and a real PostgreSQL.
 *
 * <p>All subclasses share ONE cached Spring context and ONE Postgres container. Integration tests
 * therefore run serially (see failsafe config in pom.xml) and must not rely on data created by
 * other test classes. Use {@link DatabaseCleaner} when a test needs an empty database.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class BaseIntegrationTest {}
