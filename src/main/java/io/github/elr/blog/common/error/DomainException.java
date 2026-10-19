package io.github.elr.blog.common.error;

import java.net.URI;
import org.springframework.http.HttpStatus;

/** Base class for expected business errors that map to a specific HTTP status. */
public abstract class DomainException extends RuntimeException {

    private final HttpStatus status;
    private final URI type;
    private final String title;

    protected DomainException(HttpStatus status, URI type, String title, String detail) {
        super(detail);
        this.status = status;
        this.type = type;
        this.title = title;
    }

    public HttpStatus status() {
        return status;
    }

    public URI type() {
        return type;
    }

    public String title() {
        return title;
    }
}
