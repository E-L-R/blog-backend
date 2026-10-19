package io.github.elr.blog.common.error;

import org.springframework.http.HttpStatus;

/** The request conflicts with the current state, for example a username that is already taken (409). */
public class ConflictException extends DomainException {

    public ConflictException(String detail) {
        super(HttpStatus.CONFLICT, ProblemTypes.CONFLICT, "Conflict", detail);
    }
}
