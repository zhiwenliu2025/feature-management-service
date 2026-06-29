DROP TABLE IF EXISTS idempotency_keys;

CREATE INDEX IF NOT EXISTS idx_flag_rules_conditions ON flag_rules USING GIN (conditions);

CREATE INDEX IF NOT EXISTS idx_publish_jobs_env_version ON publish_jobs (environment, config_version);

CREATE INDEX IF NOT EXISTS idx_publish_jobs_flag ON publish_jobs (flag_id, created_at DESC);
