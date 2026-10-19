package io.github.elr.blog.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.elr.blog.common.error.BadRequestException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(controllers = VersionedTestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ApiVersioningConfig.class, VersionedTestController.class})
class ApiConventionsTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    void v1_is_routed() {
        assertThat(mvc.get().uri("/api/v1/widgets"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.content[0].name")
                .isEqualTo("a");
    }

    @Test
    void unsupported_version_is_a_400_problem() {
        assertThat(mvc.get().uri("/api/v2/widgets"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void page_response_has_a_stable_shape() {
        assertThat(mvc.get().uri("/api/v1/widgets?size=2")).bodyJson().isLenientlyEqualTo("""
                        {"page": 0, "size": 2, "totalElements": 3, "totalPages": 2}
                        """);
    }

    @Test
    void sorting_by_an_unknown_field_is_rejected() {
        assertThat(mvc.get().uri("/api/v1/widgets?sort=passwordHash,asc"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.detail")
                .asString()
                .contains("Allowed: createdAt, name");
    }

    @Test
    void sanitize_caps_page_size_and_applies_the_default_sort() {
        Pageable safe = PageRequests.sanitize(PageRequest.of(0, 500), Set.of("name"), Sort.by("name"));
        assertThat(safe.getPageSize()).isEqualTo(PageRequests.MAX_PAGE_SIZE);
        assertThat(safe.getSort()).isEqualTo(Sort.by("name"));
    }

    @Test
    void sanitize_rejects_unknown_properties() {
        assertThatThrownBy(() ->
                        PageRequests.sanitize(PageRequest.of(0, 10, Sort.by("x")), Set.of("name"), Sort.unsorted()))
                .isInstanceOf(BadRequestException.class);
    }
}
