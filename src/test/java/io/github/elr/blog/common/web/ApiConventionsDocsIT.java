package io.github.elr.blog.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import io.github.elr.blog.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * The OpenAPI document shows what a client really sends (#15): the versioned path, because controllers
 * declare {@code version = "v1"} and springdoc substitutes that string into {@code {version}}, and paging
 * as {@code page}/{@code size}/{@code sort}, because list endpoints take {@code @ParameterObject Pageable}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, VersionedTestController.class})
class ApiConventionsDocsIT {

    @Autowired
    MockMvcTester mvc;

    @Test
    void api_docs_show_the_versioned_path_clients_call() {
        // user(...): an authenticated request, because the real security configuration only arrives in #17.
        assertThat(mvc.get().uri("/v3/api-docs").with(user("reader")))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.paths")
                .asMap()
                .containsKey("/api/v1/widgets")
                .doesNotContainKey("/api/1/widgets")
                .doesNotContainKey("/api/{version}/widgets");
    }

    @Test
    void paging_is_documented_as_page_size_and_sort_query_parameters() {
        // Without @ParameterObject, springdoc documents one required "pageable" object parameter instead.
        assertThat(mvc.get().uri("/v3/api-docs").with(user("reader")))
                .bodyJson()
                .extractingPath("$.paths['/api/v1/widgets'].get.parameters[*].name")
                .asArray()
                .containsExactlyInAnyOrder("page", "size", "sort");
    }
}
