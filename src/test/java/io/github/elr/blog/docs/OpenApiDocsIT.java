package io.github.elr.blog.docs;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.elr.blog.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, OpenApiDocsIT.OpenDocs.class})
class OpenApiDocsIT {

    /** Until #17 adds the real filter chain, open the docs paths for this test only. */
    @TestConfiguration(proxyBeanMethods = false)
    static class OpenDocs {
        @Bean
        SecurityFilterChain docs(HttpSecurity http) throws Exception {
            return http.securityMatcher("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                    .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                    .build();
        }
    }

    @Autowired
    MockMvcTester mvc;

    @Autowired
    BuildProperties build;

    @Test
    void api_docs_describe_the_api_and_the_bearer_scheme() {
        assertThat(mvc.get().uri("/v3/api-docs"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.info.title")
                .isEqualTo("Blog API");
        assertThat(mvc.get().uri("/v3/api-docs"))
                .bodyJson()
                .extractingPath("$.components.securitySchemes.bearerAuth.scheme")
                .isEqualTo("bearer");
    }

    @Test
    void api_docs_show_the_version_from_the_build() {
        // BuildProperties comes from spring-boot-maven-plugin's build-info goal: one source of truth, the POM.
        assertThat(mvc.get().uri("/v3/api-docs"))
                .bodyJson()
                .extractingPath("$.info.version")
                .isEqualTo(build.getVersion());
    }

    @Test
    void swagger_ui_is_served() {
        assertThat(mvc.get().uri("/swagger-ui.html")).hasStatus3xxRedirection();
    }
}
