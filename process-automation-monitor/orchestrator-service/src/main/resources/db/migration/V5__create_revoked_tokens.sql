CREATE TABLE revoked_tokens (
    id          BIGSERIAL PRIMARY KEY,
    token_hash  VARCHAR(64)  NOT NULL UNIQUE,
    revoked_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    expires_at  TIMESTAMP    NOT NULL
);

-- Index for fast look-ups on every authenticated request.
CREATE INDEX idx_revoked_tokens_hash ON revoked_tokens (token_hash);

-- Index to support the cleanup job that prunes expired entries.
CREATE INDEX idx_revoked_tokens_expires ON revoked_tokens (expires_at);
