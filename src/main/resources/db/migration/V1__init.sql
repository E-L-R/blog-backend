-- V1: baseline schema (issue #11). Never edit a migration after it has been merged:
-- write a new V<n>__description.sql instead.

CREATE TABLE users (
    id            UUID         PRIMARY KEY,
    username      VARCHAR(30)  NOT NULL,
    email         VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name  VARCHAR(100) NOT NULL,
    bio           VARCHAR(500),
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN')),
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    version       BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT users_username_format CHECK (username ~ '^[A-Za-z0-9_]{3,30}$')
);
-- Case-insensitive uniqueness: "Alice" and "alice" are the same account.
CREATE UNIQUE INDEX users_username_lower_uq ON users (lower(username));
CREATE UNIQUE INDEX users_email_lower_uq ON users (lower(email));

CREATE TABLE posts (
    id               UUID         PRIMARY KEY,
    author_id        UUID         NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    title            VARCHAR(200) NOT NULL,
    slug             VARCHAR(100) NOT NULL,
    content_markdown TEXT         NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PUBLISHED')),
    published_at     TIMESTAMPTZ,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    version          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT posts_slug_uq UNIQUE (slug),
    -- published <=> has a publication date: the database enforces the invariant too
    CONSTRAINT posts_published_at_ck CHECK ((status = 'PUBLISHED') = (published_at IS NOT NULL)),
    CONSTRAINT posts_content_length_ck CHECK (char_length(content_markdown) <= 100000)
);
-- "Latest published posts" is the hottest query: a partial index contains only published rows.
CREATE INDEX posts_published_idx ON posts (published_at DESC) WHERE status = 'PUBLISHED';
CREATE INDEX posts_author_idx ON posts (author_id, created_at DESC);

CREATE TABLE tags (
    id   UUID        PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    slug VARCHAR(60) NOT NULL,
    CONSTRAINT tags_slug_uq UNIQUE (slug)
);

CREATE TABLE post_tags (
    post_id UUID NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    tag_id  UUID NOT NULL REFERENCES tags (id) ON DELETE CASCADE,
    PRIMARY KEY (post_id, tag_id)
);
CREATE INDEX post_tags_tag_idx ON post_tags (tag_id);

CREATE TABLE comments (
    id         UUID          PRIMARY KEY,
    post_id    UUID          NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    author_id  UUID          NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    content    VARCHAR(2000) NOT NULL,
    edited     BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted    BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ   NOT NULL,
    updated_at TIMESTAMPTZ   NOT NULL,
    version    BIGINT        NOT NULL DEFAULT 0
);
CREATE INDEX comments_post_created_idx ON comments (post_id, created_at);

CREATE TABLE refresh_tokens (
    id          UUID        PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  CHAR(64)    NOT NULL,  -- SHA-256 hex of the opaque token; the token itself is never stored
    family_id   UUID        NOT NULL,  -- all tokens descending from one login
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ,
    replaced_by UUID,
    created_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT refresh_tokens_hash_uq UNIQUE (token_hash)
);
CREATE INDEX refresh_tokens_family_idx ON refresh_tokens (family_id);
CREATE INDEX refresh_tokens_user_idx ON refresh_tokens (user_id);
