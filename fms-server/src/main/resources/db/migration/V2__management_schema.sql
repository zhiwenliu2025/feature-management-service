CREATE TYPE publish_job_status AS ENUM (
    'pending', 'processing', 'completed', 'failed', 'cancelled'
);

CREATE TYPE kill_switch_scope AS ENUM ('global', 'region');

CREATE TYPE audit_action AS ENUM (
    'create', 'update', 'delete', 'publish', 'rollback',
    'promote', 'archive', 'kill_switch_on', 'kill_switch_off'
);

CREATE TABLE tags (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(64) NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE feature_flag_tags (
    flag_id     UUID NOT NULL REFERENCES feature_flags(id) ON DELETE CASCADE,
    tag_id      UUID NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (flag_id, tag_id)
);

CREATE INDEX idx_feature_flag_tags_tag ON feature_flag_tags (tag_id);

CREATE TABLE releases (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    release_id  VARCHAR(128) NOT NULL UNIQUE,
    version     VARCHAR(64) NOT NULL,
    title       VARCHAR(256),
    description TEXT,
    metadata    JSONB NOT NULL DEFAULT '{}',
    created_by  VARCHAR(256) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_releases_version ON releases (version);

CREATE TABLE flag_rules (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    flag_id         UUID NOT NULL REFERENCES feature_flags(id) ON DELETE CASCADE,
    environment     VARCHAR(32) NOT NULL REFERENCES environments(name),
    priority        INTEGER NOT NULL,
    name            VARCHAR(128),
    conditions      JSONB NOT NULL DEFAULT '{}',
    value           JSONB NOT NULL,
    is_enabled      BOOLEAN NOT NULL DEFAULT TRUE,
    schedule_start  TIMESTAMPTZ,
    schedule_end    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_flag_rules_flag_env_priority UNIQUE (flag_id, environment, priority),
    CONSTRAINT chk_flag_rules_priority_nonneg CHECK (priority >= 0)
);

CREATE INDEX idx_flag_rules_flag_env ON flag_rules (flag_id, environment);

CREATE TABLE flag_versions (
    id              BIGSERIAL PRIMARY KEY,
    flag_id         UUID NOT NULL REFERENCES feature_flags(id) ON DELETE RESTRICT,
    environment     VARCHAR(32) NOT NULL REFERENCES environments(name),
    config_version  BIGINT NOT NULL,
    flag_version    INTEGER NOT NULL,
    snapshot        JSONB NOT NULL,
    release_id      UUID REFERENCES releases(id) ON DELETE SET NULL,
    comment         TEXT,
    kill_switch     BOOLEAN NOT NULL DEFAULT FALSE,
    published_by    VARCHAR(256) NOT NULL,
    published_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_flag_versions_flag_env_version UNIQUE (flag_id, environment, flag_version),
    CONSTRAINT uq_flag_versions_env_config_flag UNIQUE (environment, config_version, flag_id)
);

CREATE INDEX idx_flag_versions_flag_env ON flag_versions (flag_id, environment, flag_version DESC);
CREATE INDEX idx_flag_versions_env_config ON flag_versions (environment, config_version DESC);

CREATE TABLE flag_environment_state (
    flag_id             UUID NOT NULL REFERENCES feature_flags(id) ON DELETE CASCADE,
    environment         VARCHAR(32) NOT NULL REFERENCES environments(name),
    is_published        BOOLEAN NOT NULL DEFAULT FALSE,
    latest_version_id   BIGINT REFERENCES flag_versions(id) ON DELETE SET NULL,
    draft_dirty         BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (flag_id, environment)
);

CREATE INDEX idx_flag_env_state_env_published
    ON flag_environment_state (environment, is_published)
    WHERE is_published = TRUE;

CREATE TABLE release_flags (
    release_id      UUID NOT NULL REFERENCES releases(id) ON DELETE CASCADE,
    flag_id         UUID NOT NULL REFERENCES feature_flags(id) ON DELETE CASCADE,
    environment     VARCHAR(32) NOT NULL REFERENCES environments(name),
    config_version  BIGINT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (release_id, flag_id)
);

CREATE INDEX idx_release_flags_flag ON release_flags (flag_id);

CREATE TABLE publish_jobs (
    id              BIGSERIAL PRIMARY KEY,
    flag_id         UUID NOT NULL REFERENCES feature_flags(id) ON DELETE RESTRICT,
    flag_key        VARCHAR(128) NOT NULL,
    environment     VARCHAR(32) NOT NULL REFERENCES environments(name),
    config_version  BIGINT NOT NULL,
    flag_version_id BIGINT REFERENCES flag_versions(id) ON DELETE RESTRICT,
    job_type        VARCHAR(32) NOT NULL DEFAULT 'publish',
    payload         JSONB NOT NULL,
    status          publish_job_status NOT NULL DEFAULT 'pending',
    attempt_count   SMALLINT NOT NULL DEFAULT 0,
    max_attempts    SMALLINT NOT NULL DEFAULT 5,
    locked_by       VARCHAR(128),
    locked_at       TIMESTAMPTZ,
    next_retry_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    error_message   TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at    TIMESTAMPTZ
);

CREATE INDEX idx_publish_jobs_pending
    ON publish_jobs (next_retry_at)
    WHERE status IN ('pending', 'failed') AND attempt_count < max_attempts;

CREATE TABLE config_version_history (
    id                  BIGSERIAL PRIMARY KEY,
    environment         VARCHAR(32) NOT NULL REFERENCES environments(name),
    config_version      BIGINT NOT NULL,
    changed_flag_ids    UUID[] NOT NULL,
    deleted_flag_keys   TEXT[] NOT NULL DEFAULT '{}',
    publish_job_id      BIGINT REFERENCES publish_jobs(id) ON DELETE SET NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_config_version_history_env_version UNIQUE (environment, config_version)
);

CREATE INDEX idx_config_version_history_env_created
    ON config_version_history (environment, created_at DESC);

CREATE TABLE kill_switch_overrides (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    flag_id         UUID NOT NULL REFERENCES feature_flags(id) ON DELETE CASCADE,
    environment     VARCHAR(32) NOT NULL REFERENCES environments(name),
    scope           kill_switch_scope NOT NULL,
    region_code     VARCHAR(8),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    forced_value    JSONB NOT NULL DEFAULT 'false',
    activated_by    VARCHAR(256) NOT NULL,
    activated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    deactivated_at  TIMESTAMPTZ,
    deactivated_by  VARCHAR(256),
    CONSTRAINT chk_kill_switch_region CHECK (
        (scope = 'global' AND region_code IS NULL) OR
        (scope = 'region' AND region_code IS NOT NULL)
    )
);

CREATE INDEX idx_kill_switch_active
    ON kill_switch_overrides (environment, flag_id)
    WHERE is_active = TRUE;

CREATE TABLE api_keys (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id  UUID NOT NULL REFERENCES applications(id) ON DELETE RESTRICT,
    name            VARCHAR(128) NOT NULL,
    key_prefix      VARCHAR(12) NOT NULL,
    key_hash        VARCHAR(128) NOT NULL,
    scopes          JSONB NOT NULL DEFAULT '["sync","evaluate"]',
    expires_at      TIMESTAMPTZ,
    revoked_at      TIMESTAMPTZ,
    created_by      VARCHAR(256) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_used_at    TIMESTAMPTZ
);

CREATE INDEX idx_api_keys_application ON api_keys (application_id);
CREATE INDEX idx_api_keys_prefix ON api_keys (key_prefix);

CREATE TABLE audit_events (
    id              BIGSERIAL PRIMARY KEY,
    actor           VARCHAR(256) NOT NULL,
    actor_ip_hash   VARCHAR(64),
    action          audit_action NOT NULL,
    resource_type   VARCHAR(64) NOT NULL,
    resource_id     VARCHAR(256) NOT NULL,
    environment     VARCHAR(32),
    request_id      VARCHAR(64),
    diff            JSONB NOT NULL,
    metadata        JSONB NOT NULL DEFAULT '{}',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_events_created ON audit_events (created_at DESC);
CREATE INDEX idx_audit_events_resource ON audit_events (resource_type, resource_id);
CREATE INDEX idx_audit_events_actor ON audit_events (actor, created_at DESC);
CREATE INDEX idx_audit_events_action ON audit_events (action, created_at DESC);

CREATE TABLE idempotency_keys (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    endpoint        VARCHAR(256) NOT NULL,
    response_status INTEGER NOT NULL,
    response_body   JSONB NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at      TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_idempotency_keys_expires ON idempotency_keys (expires_at);
