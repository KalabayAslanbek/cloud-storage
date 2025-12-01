CREATE TABLE IF NOT EXISTS folders (
  id         BIGSERIAL PRIMARY KEY,
  owner_id   BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  parent_id  BIGINT       NULL REFERENCES folders(id) ON DELETE CASCADE,
  name       VARCHAR(255) NOT NULL,
  created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
  CONSTRAINT uq_folder_sibling_name UNIQUE (owner_id, parent_id, name)
);

CREATE INDEX IF NOT EXISTS idx_folders_owner_id ON folders(owner_id);
CREATE INDEX IF NOT EXISTS idx_folders_parent_id ON folders(parent_id);

ALTER TABLE files
  ADD COLUMN folder_id BIGINT NULL;

ALTER TABLE files
  ADD CONSTRAINT fk_files_folder
    FOREIGN KEY (folder_id) REFERENCES folders(id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_files_folder_id ON files(folder_id);