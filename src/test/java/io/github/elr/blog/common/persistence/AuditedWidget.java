package io.github.elr.blog.common.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Minimal test-only entity to exercise BaseEntity before any real entity exists. */
@Entity
@Table(name = "test_audited_widgets")
public class AuditedWidget extends BaseEntity {

    private String name;

    protected AuditedWidget() {} // required by JPA

    public AuditedWidget(String name) {
        this.name = name;
    }

    public void rename(String newName) {
        this.name = newName;
    }

    public String getName() {
        return name;
    }
}
