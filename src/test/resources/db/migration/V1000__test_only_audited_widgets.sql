-- TEST-ONLY migration (src/test/resources is never packaged into the jar).
-- Backs the AuditedWidget test entity used to verify BaseEntity (issue #12).
CREATE TABLE test_audited_widgets (
    id         UUID         PRIMARY KEY,
    name       VARCHAR(50)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,
    version    BIGINT       NOT NULL
);
