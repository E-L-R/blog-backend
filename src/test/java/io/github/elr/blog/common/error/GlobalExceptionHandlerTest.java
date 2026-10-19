package io.github.elr.blog.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

// addFilters = false: these tests are about error mapping, not security (the filter chain arrives in #17).
@WebMvcTest(controllers = ProblemTestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ProblemTestController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    void domain_exception_maps_to_its_status_and_type() {
        assertThat(mvc.get().uri("/test/not-found"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.type")
                .isEqualTo(ProblemTypes.NOT_FOUND.toString());
        assertThat(mvc.get().uri("/test/not-found"))
                .bodyJson()
                .extractingPath("$.instance")
                .isEqualTo("/test/not-found");
    }

    @Test
    void conflict_is_409() {
        assertThat(mvc.get().uri("/test/conflict")).hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void optimistic_locking_failure_is_409_with_a_helpful_message() {
        assertThat(mvc.get().uri("/test/stale"))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.detail")
                .asString()
                .contains("modified by someone else");
    }

    @Test
    void data_integrity_violation_is_409_without_the_sql() {
        var result = mvc.get().uri("/test/duplicate").exchange();
        assertThat(result)
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.type")
                .isEqualTo(ProblemTypes.CONFLICT.toString());
        assertThat(result).bodyText().doesNotContain("users_username_lower_uq").doesNotContain("duplicate key");
    }

    @Test
    void constraint_violations_list_every_invalid_parameter() {
        var result = mvc.get().uri("/test/constraint-violation").exchange();
        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.type")
                .isEqualTo(ProblemTypes.VALIDATION.toString());
        assertThat(result)
                .bodyJson()
                .extractingPath("$.errors[*].field")
                .asArray()
                .containsExactly("email", "name");
    }

    @Test
    void validation_errors_list_every_invalid_field() {
        assertThat(mvc.post()
                        .uri("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "", "email": "not-an-email"}
                                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.errors[*].field")
                .asArray()
                .containsExactly("email", "name");
    }

    @Test
    void malformed_json_is_a_400_problem() {
        assertThat(mvc.post()
                        .uri("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{oops"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void unknown_route_is_a_404_problem() {
        assertThat(mvc.get().uri("/does-not-exist"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void unexpected_errors_never_leak_internals() {
        var result = mvc.get().uri("/test/boom").exchange(); // execute once, assert twice
        assertThat(result).hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(result).bodyText().doesNotContain("hunter2").doesNotContain("IllegalStateException");
    }
}
