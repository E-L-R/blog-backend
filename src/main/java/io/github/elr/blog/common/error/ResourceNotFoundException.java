package io.github.elr.blog.common.error;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String resource, Object identifier) {
        super(
                HttpStatus.NOT_FOUND,
                ProblemTypes.NOT_FOUND,
                "Resource not found",
                resource + " '" + identifier + "' not found");
    }
}
