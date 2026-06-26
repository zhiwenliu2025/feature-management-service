-- Align PostgreSQL enum columns with JPA string mappings (Hibernate 7 compatibility).

DROP INDEX IF EXISTS idx_publish_jobs_pending;
DROP INDEX IF EXISTS idx_kill_switch_active;

ALTER TABLE feature_flags
    ALTER COLUMN status DROP DEFAULT,
    ALTER COLUMN type DROP DEFAULT;

ALTER TABLE applications
    ALTER COLUMN status DROP DEFAULT;

ALTER TABLE publish_jobs
    ALTER COLUMN status DROP DEFAULT;

ALTER TABLE kill_switch_overrides
    ALTER COLUMN scope DROP DEFAULT;

ALTER TABLE kill_switch_overrides
    DROP CONSTRAINT IF EXISTS chk_kill_switch_region;

ALTER TABLE feature_flags
    ALTER COLUMN status TYPE VARCHAR(32) USING status::text,
    ALTER COLUMN type TYPE VARCHAR(32) USING type::text;

ALTER TABLE applications
    ALTER COLUMN status TYPE VARCHAR(32) USING status::text;

ALTER TABLE audit_events
    ALTER COLUMN action TYPE VARCHAR(32) USING action::text;

ALTER TABLE publish_jobs
    ALTER COLUMN status TYPE VARCHAR(32) USING status::text;

ALTER TABLE kill_switch_overrides
    ALTER COLUMN scope TYPE VARCHAR(32) USING scope::text;

ALTER TABLE kill_switch_overrides
    ADD CONSTRAINT chk_kill_switch_region CHECK (
        (scope = 'global' AND region_code IS NULL) OR
        (scope = 'region' AND region_code IS NOT NULL)
    );

ALTER TABLE feature_flags
    ALTER COLUMN status SET DEFAULT 'draft',
    ALTER COLUMN type SET DEFAULT 'boolean';

ALTER TABLE applications
    ALTER COLUMN status SET DEFAULT 'active';

ALTER TABLE publish_jobs
    ALTER COLUMN status SET DEFAULT 'pending';

CREATE INDEX idx_publish_jobs_pending
    ON publish_jobs (next_retry_at)
    WHERE status IN ('pending', 'failed') AND attempt_count < max_attempts;

CREATE INDEX idx_kill_switch_active
    ON kill_switch_overrides (environment, flag_id)
    WHERE is_active = TRUE;

DROP TYPE IF EXISTS publish_job_status;
DROP TYPE IF EXISTS kill_switch_scope;
DROP TYPE IF EXISTS audit_action;
DROP TYPE IF EXISTS flag_status;
DROP TYPE IF EXISTS flag_type;
DROP TYPE IF EXISTS application_status;
