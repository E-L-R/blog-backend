package io.github.elr.blog;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.elr.blog.config.BlogProperties;
import io.github.elr.blog.testsupport.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

/** Regression test for #78: integration tests must run with the "test" profile. */
class TestProfileIT extends BaseIntegrationTest {

    @Autowired
    Environment environment;

    @Autowired
    BlogProperties properties;

    @Test
    void test_profile_is_active() {
        assertThat(environment.getActiveProfiles()).contains("test");
    }

    @Test
    void test_profile_values_are_applied() {
        // value only present in application-test.yaml
        assertThat(properties.jwt().issuer()).isEqualTo("blog-test");
    }
}
