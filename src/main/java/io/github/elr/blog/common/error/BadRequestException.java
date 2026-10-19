package io.github.elr.blog.common.error;

import org.springframework.http.HttpStatus;

/** A well-formed request that can't be carried out as asked, for example an unknown sort field (400). */
public class BadRequestException extends DomainException {

    public BadRequestException(String detail) {
        super(HttpStatus.BAD_REQUEST, ProblemTypes.BAD_REQUEST, "Bad request", detail);
    }
}
