package io.github.elr.blog.common.error;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.sql.SQLException;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Test-only endpoints that trigger each error path. Lives in src/test, so it never ships. */
@TestComponent // excluded from component scanning: only tests that @Import it see this controller
@RestController
class ProblemTestController {

    record Payload(
            @NotBlank @Size(max = 10) String name, @Email String email) {}

    private final Validator validator;

    ProblemTestController(Validator validator) {
        this.validator = validator;
    }

    @GetMapping("/test/not-found")
    void notFound() {
        throw new ResourceNotFoundException("Post", "missing-slug");
    }

    @GetMapping("/test/conflict")
    void conflict() {
        throw new ConflictException("Username is already taken");
    }

    @GetMapping("/test/stale")
    void stale() {
        throw new OptimisticLockingFailureException("row was updated by another transaction");
    }

    @GetMapping("/test/duplicate")
    void duplicate() {
        // What a lost race on a unique index looks like: the driver's message names the constraint.
        throw new DataIntegrityViolationException(
                "could not execute statement",
                new SQLException("ERROR: duplicate key value violates unique constraint \"users_username_lower_uq\""));
    }

    @GetMapping("/test/constraint-violation")
    void constraintViolation() {
        // What method validation (a @Validated bean) throws for invalid parameters.
        throw new ConstraintViolationException(validator.validate(new Payload("", "not-an-email")));
    }

    @GetMapping("/test/boom")
    void boom() {
        throw new IllegalStateException("password=hunter2 leaked in an exception message");
    }

    @PostMapping("/test/validate")
    Payload validate(@Valid @RequestBody Payload payload) {
        return payload;
    }
}
