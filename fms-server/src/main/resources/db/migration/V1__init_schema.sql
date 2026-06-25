CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TYPE application_status AS ENUM ('active', 'inactive', 'suspended');
CREATE TYPE flag_status AS ENUM ('draft', 'published', 'archived');
CREATE TYPE flag_type AS ENUM ('boolean', 'string', 'number', 'json');

CREATE TABLE environments (
    name          VARCHAR(32)  PRIMARY KEY,
    display_name  VARCHAR(64)  NOT NULL,
    sort_order    SMALLINT     NOT NULL DEFAULT 0,
    is_production BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

INSERT INTO environments (name, display_name, sort_order, is_production) VALUES
    ('dev', 'Development', 1, FALSE),
    ('staging', 'Staging', 2, FALSE),
    ('prod', 'Production', 3, TRUE);

CREATE TABLE applications (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug         VARCHAR(64)  NOT NULL UNIQUE,
    name         VARCHAR(128) NOT NULL,
    description  TEXT,
    status       application_status NOT NULL DEFAULT 'active',
    owner_team   VARCHAR(128),
    created_by   VARCHAR(256) NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE feature_flags (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id  UUID NOT NULL REFERENCES applications(id) ON DELETE RESTRICT,
    key             VARCHAR(128) NOT NULL,
    name            VARCHAR(256) NOT NULL,
    description     TEXT,
    type            flag_type NOT NULL DEFAULT 'boolean',
    default_value   JSONB NOT NULL,
    status          flag_status NOT NULL DEFAULT 'draft',
    rollout_salt    VARCHAR(64) NOT NULL,
    created_by      VARCHAR(256) NOT NULL,
    updated_by      VARCHAR(256),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_feature_flags_app_key UNIQUE (application_id, key),
    CONSTRAINT chk_feature_flags_key_format CHECK (key ~ '^[a-z][a-z0-9_]{0,127}$')
);

CREATE INDEX idx_feature_flags_application ON feature_flags (application_id);
CREATE INDEX idx_feature_flags_status ON feature_flags (status);
CREATE INDEX idx_feature_flags_app_status ON feature_flags (application_id, status);

CREATE TABLE environment_config (
    environment            VARCHAR(32) PRIMARY KEY REFERENCES environments(name),
    current_config_version BIGINT NOT NULL DEFAULT 0,
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO environment_config (environment, current_config_version) VALUES
    ('dev', 0),
    ('staging', 0),
    ('prod', 0);

-- Seed demo application for local development
INSERT INTO applications (slug, name, description, created_by)
VALUES ('checkout-service', 'Checkout Service', 'Demo application', 'system');
