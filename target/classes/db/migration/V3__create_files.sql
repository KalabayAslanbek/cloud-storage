CREATE TABLE IF NOT EXISTS files (
  id            BIGSERIAL PRIMARY KEY,
  owner_id      BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  original_name VARCHAR(255) NOT NULL,
  storage_name  VARCHAR(64)  NOT NULL UNIQUE,
  content_type  VARCHAR(255),
  size_bytes    BIGINT       NOT NULL,
  uploaded_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_files_owner_id ON files(owner_id);