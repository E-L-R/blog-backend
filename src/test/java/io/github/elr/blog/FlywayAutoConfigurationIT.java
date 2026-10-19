package io.github.elr.blog;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.elr.blog.testsupport.BaseIntegrationTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/**
 * Regression test for #77: on Spring Boot 4, Flyway's auto-configuration lives in the
 * spring-boot-flyway module. Without it, migrations silently never run.
 */
class FlywayAutoConfigurationIT extends BaseIntegrationTest {

    @Autowired
    ApplicationContext context;

    @Test
    void flyway_is_auto_configured() {
        assertThat(context.getBeanProvider(Flyway.class).getIfAvailable())
                .as("Flyway bean should be auto-configured (is spring-boot-starter-flyway on the classpath?)")
                .isNotNull();
    }
}
