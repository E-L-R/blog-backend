package io.github.elr.blog.common.error;

import org.springframework.http.HttpStatus;

/** The caller is authenticated but not allowed to do this, for example edit someone else's post (403). */
public class ForbiddenOperationException extends DomainException {

    public ForbiddenOperationException(String detail) {
        super(HttpStatus.FORBIDDEN, ProblemTypes.FORBIDDEN, "Forbidden", detail);
    }
}
