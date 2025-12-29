CREATE TABLE IF NOT EXISTS file_shares (
    id          BIGSERIAL PRIMARY KEY,
    file_id     BIGINT      NOT NULL REFERENCES files(id) ON DELETE CASCADE,
    token       VARCHAR(64) NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at  TIMESTAMPTZ NULL,
    revoked     BOOLEAN     NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_file_shares_token ON file_shares(token);
CREATE INDEX IF NOT EXISTS idx_file_shares_file_id ON file_shares(file_id);