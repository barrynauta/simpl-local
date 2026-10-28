CREATE TABLE IF NOT EXISTS rd (
  id VARCHAR(2048) PRIMARY KEY,
  sd_hash VARCHAR(64) NOT NULL UNIQUE,
  issuer VARCHAR(4096) NOT NULL,
  provided_by VARCHAR(1024),
  status VARCHAR(16) NOT NULL,
  upload_ms BIGINT NOT NULL,
  status_ms BIGINT NOT NULL,
  resource_key VARCHAR(2048) NOT NULL,
  version INTEGER NOT NULL,
  schema_name VARCHAR(256),
  content TEXT NOT NULL,
  qar TEXT,
  quality TEXT,
  overall_score DOUBLE PRECISION,
  overall_class VARCHAR(256)
);
CREATE INDEX IF NOT EXISTS rd_status_idx ON rd (status);
CREATE INDEX IF NOT EXISTS rd_key_idx ON rd (resource_key);
CREATE TABLE IF NOT EXISTS shape (
  schema_id VARCHAR(256) PRIMARY KEY,
  version VARCHAR(64) NOT NULL,
  status VARCHAR(16) NOT NULL,
  content TEXT NOT NULL,
  created_ms BIGINT NOT NULL
);
CREATE TABLE IF NOT EXISTS schema_status (
  name VARCHAR(256) PRIMARY KEY,
  status VARCHAR(16) NOT NULL,
  version VARCHAR(64)
);
