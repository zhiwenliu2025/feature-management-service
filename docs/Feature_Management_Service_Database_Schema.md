# Feature Management Service — Database Schema Design

| Attribute | Value |
|-----------|-------|
| **Document Version** | 1.1 |
| **Status** | As-built (aligned with Flyway V1–V5) |
| **Created** | 2026-06-25 |
| **Last Updated** | 2026-06-29 |
| **Database** | PostgreSQL 18.4 |
| **Migration Tool** | Flyway 12.4.0 |
| **Migration Location** | `fms-server/src/main/resources/db/migration/` |
| **Related Documents** | [BRD](./Feature_Management_Service_BRD.md) · [Technical Architecture](./Feature_Management_Service_Technical_Architecture.md) · [Redis Cache Design](./Feature_Management_Service_Redis_Cache_Design.md) · [Technology Stack](./Feature_Management_Service_Technology_Stack.md) |

---

## 1. Purpose

This document defines the **PostgreSQL relational data model** for the Feature Management Service (FMS). It serves as the authoritative table-structure reference during implementation and reflects the **as-built schema** from Flyway migrations V1–V5. It covers:

- Entity relationships and table inventory
- Column definitions, constraints, and indexing strategy
- JSONB column structure conventions
- Version numbering and publish Outbox mechanics
- Responsibility boundaries between PostgreSQL and the Redis cache layer

**Out of scope**: Redis key patterns and cache design are documented in [Redis Cache Design](./Feature_Management_Service_Redis_Cache_Design.md). Explain results are computed at runtime and are **not persisted**.

---

## 2. Design Principles

| Principle | Description |
|-----------|-------------|
| **Control-plane strong consistency** | Management writes commit in a single transaction; `flag_versions`, `audit_events`, and `publish_jobs` are written together |
| **Immutable versions** | Published `flag_versions` rows are append-only; rollback = publish a new version from an old snapshot |
| **Environment isolation** | `config_version` increments monotonically per **environment**; environments are independent |
| **Per-application subscription** | All flags belong to `applications`; SDKs subscribe to a subset by `appId` |
| **Append-only audit** | `audit_events` forbids UPDATE/DELETE (application layer + optional DB triggers) |
| **No plaintext secrets in storage** | API keys store hashes only; large sensitive lists (e.g., user IDs) use an external Segment service (MVP may inline in JSONB with size limits) |

---

## 3. Entity Relationship Diagram (ERD)

```mermaid
erDiagram
    applications ||--o{ feature_flags : owns
    applications ||--o{ api_keys : has
    feature_flags ||--o{ flag_rules : defines
    feature_flags ||--o{ flag_versions : versions
    feature_flags ||--o{ kill_switch_overrides : overrides
    feature_flags }o--o{ tags : tagged
    feature_flags }o--o{ releases : linked_via
    releases ||--o{ release_flags : contains
    feature_flags ||--o{ release_flags : referenced
    flag_versions }o--o| releases : bound_to
    environments ||--o{ flag_rules : scopes
    environments ||--|| environment_config : tracks
    environments ||--o{ publish_jobs : queues
    environments ||--o{ config_version_history : logs
    feature_flags ||--o{ publish_jobs : triggers

    applications {
        uuid id PK
        varchar slug UK
        varchar name
        varchar status
    }

    feature_flags {
        uuid id PK
        uuid application_id FK
        varchar key
        varchar type
        jsonb default_value
        varchar status
    }

    flag_rules {
        uuid id PK
        uuid flag_id FK
        varchar environment FK
        int priority
        jsonb conditions
        jsonb value
    }

    flag_versions {
        bigint id PK
        uuid flag_id FK
        varchar environment FK
        bigint config_version
        jsonb snapshot
        uuid release_id FK
    }

    publish_jobs {
        bigint id PK
        varchar environment FK
        bigint config_version
        varchar status
    }

    audit_events {
        bigint id PK
        varchar actor
        varchar action
        jsonb diff
    }
```

---

## 4. Table Inventory

| # | Table | Type | Description |
|---|-------|------|-------------|
| 1 | `environments` | Reference | Environment definitions (dev / staging / prod) |
| 2 | `applications` | Master | Applications/services onboarded to FMS |
| 3 | `api_keys` | Master | Data-plane API keys (hashed storage) |
| 4 | `tags` | Master | Tag dictionary |
| 5 | `feature_flags` | Core | Feature flag metadata |
| 6 | `feature_flag_tags` | Junction | Flag ↔ Tag many-to-many |
| 7 | `flag_rules` | Core | Editable rules (pre-publish working copy) |
| 8 | `flag_environment_state` | Core | Per-environment publish state per flag |
| 9 | `releases` | Master | Release ticket / version association |
| 10 | `release_flags` | Junction | Release ↔ Flag binding |
| 11 | `flag_versions` | Core | Immutable published snapshot history |
| 12 | `environment_config` | Core | Per-environment current `config_version` pointer |
| 13 | `config_version_history` | Auxiliary | Environment-level version change log (incremental sync metadata) |
| 14 | `kill_switch_overrides` | Core | Emergency kill switch (global / regional) |
| 15 | `publish_jobs` | Outbox | Async publish job queue |
| 16 | `audit_events` | Audit | Management-plane audit log |
| 17 | `idempotency_records` | Auxiliary | Management API idempotency cache |

---

## 5. Enums and Domain Types

### 5.1 Storage vs Application Layer

| Layer | Mechanism | Notes |
|-------|-----------|-------|
| **PostgreSQL** | `VARCHAR(32)` | All former ENUM columns were converted in `V3__enum_columns_to_varchar.sql`; PostgreSQL ENUM types are **not** present in the as-built schema |
| **Application** | Java enums in `com.fms.domain.enums` | Hibernate maps `@Enumerated(EnumType.STRING)` to `VARCHAR`; this is the authoritative value set at runtime |

### 5.2 Domain Value Sets

| Column(s) | Java Enum | Allowed Values |
|-----------|-----------|----------------|
| `applications.status` | `ApplicationStatus` | `active`, `inactive`, `suspended` |
| `feature_flags.status` | `FlagStatus` | `draft`, `published`, `archived` |
| `feature_flags.type` | `FlagType` | `boolean`, `string`, `number`, `json` |
| `publish_jobs.status` | `PublishJobStatus` | `pending`, `processing`, `completed`, `failed`, `cancelled` |
| `publish_jobs.job_type` | `PublishJobType` | `publish`, `rollback`, `promote` |
| `kill_switch_overrides.scope` | `KillSwitchScope` | `global`, `region` |
| `audit_events.action` | `AuditAction` | `create`, `update`, `delete`, `publish`, `rollback`, `promote`, `archive`, `kill_switch_on`, `kill_switch_off` |

> **Migration history**: `V1` and `V2` originally created PostgreSQL ENUM types; `V3` dropped them after converting affected columns to `VARCHAR(32)` for Hibernate 7 compatibility.

### 5.3 Naming Conventions

| Convention | Rule |
|------------|------|
| Primary keys | `UUID` (`gen_random_uuid()`) for business entities; `BIGSERIAL` for high-write log tables |
| Timestamps | `TIMESTAMPTZ`, always UTC |
| Soft delete | Not used; archival via `feature_flags.status = 'archived'` |
| FK on delete | `RESTRICT` for business entities; `CASCADE` for junction tables; `RESTRICT` for audit/history |

---

## 6. Detailed Table Definitions

### 6.1 `environments` — Environments

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `name` | `VARCHAR(32)` | PK | Environment identifier: `dev`, `staging`, `prod` |
| `display_name` | `VARCHAR(64)` | NOT NULL | Display name |
| `sort_order` | `SMALLINT` | NOT NULL DEFAULT 0 | Sort order (promote direction) |
| `is_production` | `BOOLEAN` | NOT NULL DEFAULT FALSE | Whether this is a production environment |
| `created_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT `now()` | Created timestamp |

**Seed data** (`V1__init_schema.sql`): `dev`, `staging`, `prod` (`prod.is_production = true`); `environment_config` rows initialized to `current_config_version = 0`; demo application `checkout-service` for local development.

---

### 6.2 `applications` — Applications

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | `UUID` | PK, DEFAULT `gen_random_uuid()` | Primary key |
| `slug` | `VARCHAR(64)` | NOT NULL, UNIQUE | `appId` used by SDK/API |
| `name` | `VARCHAR(128)` | NOT NULL | Application name |
| `description` | `TEXT` | | Description |
| `status` | `VARCHAR(32)` | NOT NULL DEFAULT `'active'` | Status (`ApplicationStatus`) |
| `owner_team` | `VARCHAR(128)` | | Owning team |
| `created_by` | `VARCHAR(256)` | NOT NULL | Creator (OIDC subject or email) |
| `created_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT `now()` | |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT `now()` | |

**Indexes**:

```sql
CREATE INDEX idx_applications_status ON applications (status);
-- idx_applications_slug: not migrated; UNIQUE on slug already provides lookup
```

---

### 6.3 `api_keys` — API Keys

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | `UUID` | PK | Primary key |
| `application_id` | `UUID` | NOT NULL, FK → `applications(id)` ON DELETE RESTRICT | Owning application |
| `name` | `VARCHAR(128)` | NOT NULL | Key purpose description |
| `key_prefix` | `VARCHAR(12)` | NOT NULL | Plaintext prefix for identification (e.g., `fms_a1b2`) |
| `key_hash` | `VARCHAR(128)` | NOT NULL | Argon2/bcrypt hash; **never store plaintext** |
| `scopes` | `JSONB` | NOT NULL DEFAULT `'["sync","evaluate"]'` | Permission scopes |
| `expires_at` | `TIMESTAMPTZ` | | Expiration time |
| `revoked_at` | `TIMESTAMPTZ` | | Revocation time |
| `created_by` | `VARCHAR(256)` | NOT NULL | |
| `created_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT `now()` | |
| `last_used_at` | `TIMESTAMPTZ` | | Last used timestamp |

**Indexes**:

```sql
CREATE INDEX idx_api_keys_application ON api_keys (application_id);
CREATE INDEX idx_api_keys_prefix ON api_keys (key_prefix);
-- Planned (not in V1–V5): partial index for active keys only
-- CREATE INDEX idx_api_keys_active ON api_keys (application_id)
--     WHERE revoked_at IS NULL AND (expires_at IS NULL OR expires_at > now());
```

---

### 6.4 `tags` — Tags

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | `UUID` | PK | |
| `name` | `VARCHAR(64)` | NOT NULL, UNIQUE | Tag name (normalized lowercase) |
| `created_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT `now()` | |

---

### 6.5 `feature_flags` — Feature Flags

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | `UUID` | PK | |
| `application_id` | `UUID` | NOT NULL, FK → `applications(id)` ON DELETE RESTRICT | Owning application |
| `key` | `VARCHAR(128)` | NOT NULL | Unique flag key (unique per application) |
| `name` | `VARCHAR(256)` | NOT NULL | Display name |
| `description` | `TEXT` | | Description |
| `type` | `VARCHAR(32)` | NOT NULL DEFAULT `'boolean'` | Value type (`FlagType`) |
| `default_value` | `JSONB` | NOT NULL | Default value (e.g., `false`, `"control"`) |
| `status` | `VARCHAR(32)` | NOT NULL DEFAULT `'draft'` | Global lifecycle status (`FlagStatus`) |
| `rollout_salt` | `VARCHAR(64)` | NOT NULL | Stable bucketing salt (written into snapshot on publish) |
| `created_by` | `VARCHAR(256)` | NOT NULL | |
| `updated_by` | `VARCHAR(256)` | | |
| `created_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT `now()` | |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT `now()` | |

**Constraints**:

```sql
ALTER TABLE feature_flags
    ADD CONSTRAINT uq_feature_flags_app_key UNIQUE (application_id, key);

ALTER TABLE feature_flags
    ADD CONSTRAINT chk_feature_flags_key_format
    CHECK (key ~ '^[a-z][a-z0-9_]{0,127}$');
```

**Indexes**:

```sql
CREATE INDEX idx_feature_flags_application ON feature_flags (application_id);
CREATE INDEX idx_feature_flags_status ON feature_flags (status);
CREATE INDEX idx_feature_flags_app_status ON feature_flags (application_id, status);
```

---

### 6.6 `feature_flag_tags` — Flag–Tag Association

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `flag_id` | `UUID` | PK, FK → `feature_flags(id)` ON DELETE CASCADE | |
| `tag_id` | `UUID` | PK, FK → `tags(id)` ON DELETE CASCADE | |
| `created_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT `now()` | |

```sql
CREATE INDEX idx_feature_flag_tags_tag ON feature_flag_tags (tag_id);
```

---

### 6.7 `flag_rules` — Evaluation Rules (Working Copy)

Editable rule drafts via the Management API; **on publish**, compiled into `flag_versions.snapshot`. Runtime evaluation uses published snapshots.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | `UUID` | PK | |
| `flag_id` | `UUID` | NOT NULL, FK → `feature_flags(id)` ON DELETE CASCADE | |
| `environment` | `VARCHAR(32)` | NOT NULL, FK → `environments(name)` | Target environment |
| `priority` | `INTEGER` | NOT NULL | Priority; evaluated in **ascending** order; first match wins |
| `name` | `VARCHAR(128)` | | Rule name |
| `conditions` | `JSONB` | NOT NULL DEFAULT `'{}'` | Match conditions (see §8.1) |
| `value` | `JSONB` | NOT NULL | Return value when matched |
| `is_enabled` | `BOOLEAN` | NOT NULL DEFAULT TRUE | Whether rule participates in evaluation |
| `schedule_start` | `TIMESTAMPTZ` | | Scheduled activation start (P2) |
| `schedule_end` | `TIMESTAMPTZ` | | Scheduled activation end |
| `created_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT `now()` | |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT `now()` | |

**Constraints**:

```sql
ALTER TABLE flag_rules
    ADD CONSTRAINT uq_flag_rules_flag_env_priority
    UNIQUE (flag_id, environment, priority);

ALTER TABLE flag_rules
    ADD CONSTRAINT chk_flag_rules_priority_nonneg
    CHECK (priority >= 0);
```

**Indexes**:

```sql
CREATE INDEX idx_flag_rules_flag_env ON flag_rules (flag_id, environment);
CREATE INDEX idx_flag_rules_conditions ON flag_rules USING GIN (conditions);  -- V5
```

---

### 6.8 `flag_environment_state` — Per-Environment Publish State

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `flag_id` | `UUID` | PK, FK → `feature_flags(id)` ON DELETE CASCADE | |
| `environment` | `VARCHAR(32)` | PK, FK → `environments(name)` | |
| `is_published` | `BOOLEAN` | NOT NULL DEFAULT FALSE | Whether published in this environment |
| `latest_version_id` | `BIGINT` | FK → `flag_versions(id)` ON DELETE SET NULL | Currently active version |
| `draft_dirty` | `BOOLEAN` | NOT NULL DEFAULT TRUE | Whether rules have unpublished changes |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT `now()` | |

```sql
CREATE INDEX idx_flag_env_state_env_published
    ON flag_environment_state (environment, is_published)
    WHERE is_published = TRUE;
```

---

### 6.9 `releases` — Releases

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | `UUID` | PK | |
| `release_id` | `VARCHAR(128)` | NOT NULL, UNIQUE | External release ticket ID (e.g., `REL-2026-06-25-checkout`) |
| `version` | `VARCHAR(64)` | NOT NULL | Semantic version or build number |
| `title` | `VARCHAR(256)` | | Title |
| `description` | `TEXT` | | |
| `metadata` | `JSONB` | NOT NULL DEFAULT `'{}'` | CI/CD metadata |
| `created_by` | `VARCHAR(256)` | NOT NULL | |
| `created_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT `now()` | |

```sql
CREATE INDEX idx_releases_version ON releases (version);
```

---

### 6.10 `release_flags` — Release–Flag Association

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `release_id` | `UUID` | PK, FK → `releases(id)` ON DELETE CASCADE | |
| `flag_id` | `UUID` | PK, FK → `feature_flags(id)` ON DELETE CASCADE | |
| `environment` | `VARCHAR(32)` | NOT NULL, FK → `environments(name)` | Bound environment |
| `config_version` | `BIGINT` | | Environment config version at bind time |
| `created_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT `now()` | |

```sql
CREATE INDEX idx_release_flags_flag ON release_flags (flag_id);
```

---

### 6.11 `flag_versions` — Published Version Snapshots (Immutable)

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | `BIGSERIAL` | PK | |
| `flag_id` | `UUID` | NOT NULL, FK → `feature_flags(id)` ON DELETE RESTRICT | |
| `environment` | `VARCHAR(32)` | NOT NULL, FK → `environments(name)` | |
| `config_version` | `BIGINT` | NOT NULL | **Environment-level** monotonic version number |
| `flag_version` | `INTEGER` | NOT NULL | Per-flag version sequence (starts at 1) |
| `snapshot` | `JSONB` | NOT NULL | Compiled full snapshot (see §8.2) |
| `release_id` | `UUID` | FK → `releases(id)` ON DELETE SET NULL | Linked release |
| `comment` | `TEXT` | | Publish comment |
| `kill_switch` | `BOOLEAN` | NOT NULL DEFAULT FALSE | Whether this publish includes a kill switch |
| `published_by` | `VARCHAR(256)` | NOT NULL | |
| `published_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT `now()` | |

**Constraints**:

```sql
ALTER TABLE flag_versions
    ADD CONSTRAINT uq_flag_versions_flag_env_version
    UNIQUE (flag_id, environment, flag_version);

ALTER TABLE flag_versions
    ADD CONSTRAINT uq_flag_versions_env_config_flag
    UNIQUE (environment, config_version, flag_id);
```

**Indexes**:

```sql
CREATE INDEX idx_flag_versions_flag_env ON flag_versions (flag_id, environment, flag_version DESC);
CREATE INDEX idx_flag_versions_env_config ON flag_versions (environment, config_version DESC);
-- Planned (not in V1–V5):
-- CREATE INDEX idx_flag_versions_published_at ON flag_versions (published_at DESC);
-- CREATE INDEX idx_flag_versions_release ON flag_versions (release_id) WHERE release_id IS NOT NULL;
```

---

### 6.12 `environment_config` — Current Environment Version

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `environment` | `VARCHAR(32)` | PK, FK → `environments(name)` | |
| `current_config_version` | `BIGINT` | NOT NULL DEFAULT 0 | Current latest config version |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT `now()` | |

> On publish, within the same transaction: `current_config_version = current_config_version + 1` to ensure monotonic increment.

---

### 6.13 `config_version_history` — Environment Version Log

Records metadata for each `config_version` increment; supports incremental sync and Explain replay.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | `BIGSERIAL` | PK | |
| `environment` | `VARCHAR(32)` | NOT NULL, FK → `environments(name)` | |
| `config_version` | `BIGINT` | NOT NULL | |
| `changed_flag_ids` | `UUID[]` | NOT NULL | Flag IDs changed in this version |
| `deleted_flag_keys` | `TEXT[]` | NOT NULL DEFAULT `'{}'` | Flag keys deleted/archived in this version |
| `publish_job_id` | `BIGINT` | FK → `publish_jobs(id)` ON DELETE SET NULL | |
| `created_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT `now()` | |

**Constraints**:

```sql
ALTER TABLE config_version_history
    ADD CONSTRAINT uq_config_version_history_env_version
    UNIQUE (environment, config_version);
```

**Indexes**:

```sql
CREATE INDEX idx_config_version_history_env_created
    ON config_version_history (environment, created_at DESC);
```

**Retention policy**: Retain the most recent **500** versions (configurable); beyond that, SDKs must perform a full sync.

---

### 6.14 `kill_switch_overrides` — Emergency Kill Switch

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | `UUID` | PK | |
| `flag_id` | `UUID` | NOT NULL, FK → `feature_flags(id)` ON DELETE CASCADE | |
| `environment` | `VARCHAR(32)` | NOT NULL, FK → `environments(name)` | |
| `scope` | `VARCHAR(32)` | NOT NULL | `global` or `region` (`KillSwitchScope`) |
| `region_code` | `VARCHAR(8)` | | Required when `scope=region` (ISO 3166-1 alpha-2) |
| `is_active` | `BOOLEAN` | NOT NULL DEFAULT TRUE | Whether override is active |
| `forced_value` | `JSONB` | NOT NULL DEFAULT `'false'` | Forced return value (typically `false`) |
| `activated_by` | `VARCHAR(256)` | NOT NULL | |
| `activated_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT `now()` | |
| `deactivated_at` | `TIMESTAMPTZ` | | Deactivation time |
| `deactivated_by` | `VARCHAR(256)` | | |

**Constraints**:

```sql
ALTER TABLE kill_switch_overrides
    ADD CONSTRAINT chk_kill_switch_region
    CHECK (
        (scope = 'global' AND region_code IS NULL) OR
        (scope = 'region' AND region_code IS NOT NULL)
    );
```

**Indexes**:

```sql
CREATE INDEX idx_kill_switch_active
    ON kill_switch_overrides (environment, flag_id)
    WHERE is_active = TRUE;
```

---

### 6.15 `publish_jobs` — Publish Outbox

Transactional Outbox pattern; consumed asynchronously by the Publish Worker after Management API commit.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | `BIGSERIAL` | PK | |
| `flag_id` | `UUID` | NOT NULL, FK → `feature_flags(id)` ON DELETE RESTRICT | |
| `flag_key` | `VARCHAR(128)` | NOT NULL | Denormalized for worker logs and monitoring |
| `environment` | `VARCHAR(32)` | NOT NULL, FK → `environments(name)` | |
| `config_version` | `BIGINT` | NOT NULL | Allocated environment version number |
| `flag_version_id` | `BIGINT` | FK → `flag_versions(id)` ON DELETE RESTRICT | Linked snapshot row |
| `job_type` | `VARCHAR(32)` | NOT NULL DEFAULT `'publish'` | `PublishJobType`: `publish`, `rollback`, `promote` |
| `payload` | `JSONB` | NOT NULL | Compilation context required by worker |
| `status` | `VARCHAR(32)` | NOT NULL DEFAULT `'pending'` | `PublishJobStatus` |
| `attempt_count` | `SMALLINT` | NOT NULL DEFAULT 0 | |
| `max_attempts` | `SMALLINT` | NOT NULL DEFAULT 5 | |
| `locked_by` | `VARCHAR(128)` | | Worker instance ID |
| `locked_at` | `TIMESTAMPTZ` | | |
| `next_retry_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT `now()` | |
| `error_message` | `TEXT` | | Most recent failure reason |
| `created_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT `now()` | |
| `completed_at` | `TIMESTAMPTZ` | | |

**Indexes**:

```sql
-- Worker polling: pending and retryable
CREATE INDEX idx_publish_jobs_pending
    ON publish_jobs (next_retry_at)
    WHERE status IN ('pending', 'failed') AND attempt_count < max_attempts;

CREATE INDEX idx_publish_jobs_env_version ON publish_jobs (environment, config_version);  -- V5
CREATE INDEX idx_publish_jobs_flag ON publish_jobs (flag_id, created_at DESC);           -- V5
```

**Worker claim SQL (reference)**:

```sql
UPDATE publish_jobs
SET status = 'processing',
    locked_by = :workerId,
    locked_at = now(),
    attempt_count = attempt_count + 1
WHERE id = (
    SELECT id FROM publish_jobs
    WHERE status IN ('pending', 'failed')
      AND attempt_count < max_attempts
      AND next_retry_at <= now()
    ORDER BY created_at
    FOR UPDATE SKIP LOCKED
    LIMIT 1
)
RETURNING *;
```

---

### 6.16 `audit_events` — Audit Log

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | `BIGSERIAL` | PK | |
| `actor` | `VARCHAR(256)` | NOT NULL | Actor |
| `actor_ip_hash` | `VARCHAR(64)` | | Hashed request IP (not plaintext IP) |
| `action` | `VARCHAR(32)` | NOT NULL | Action type (`AuditAction`) |
| `resource_type` | `VARCHAR(64)` | NOT NULL | e.g., `feature_flag`, `release` |
| `resource_id` | `VARCHAR(256)` | NOT NULL | Resource identifier |
| `environment` | `VARCHAR(32)` | | Related environment |
| `request_id` | `VARCHAR(64)` | | `X-Request-Id` correlation |
| `diff` | `JSONB` | NOT NULL | Before/after diff |
| `metadata` | `JSONB` | NOT NULL DEFAULT `'{}'` | Extension fields |
| `created_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT `now()` | |

**Indexes**:

```sql
CREATE INDEX idx_audit_events_created ON audit_events (created_at DESC);
CREATE INDEX idx_audit_events_resource ON audit_events (resource_type, resource_id);
CREATE INDEX idx_audit_events_actor ON audit_events (actor, created_at DESC);
CREATE INDEX idx_audit_events_action ON audit_events (action, created_at DESC);
```

**Retention policy**: Online retention **13 months**; older records archived to cold storage (S3 / audit platform).

---

### 6.17 `idempotency_records` — Management API Idempotency

Caches successful Management API responses keyed by `Idempotency-Key` header and request operation identity.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | `UUID` | PK | |
| `idempotency_key` | `VARCHAR(128)` | NOT NULL | Client-supplied idempotency key |
| `operation_key` | `VARCHAR(512)` | NOT NULL | `METHOD path[?query]` operation identity |
| `response_status` | `INT` | NOT NULL | Cached HTTP status |
| `response_body` | `JSONB` | NOT NULL | Cached response body |
| `response_headers` | `JSONB` | | Cached response headers |
| `created_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT `now()` | |
| `expires_at` | `TIMESTAMPTZ` | NOT NULL | TTL expiry |

**Constraints**:

```sql
CONSTRAINT uq_idempotency_key_operation UNIQUE (idempotency_key, operation_key)
```

**Indexes**:

```sql
CREATE INDEX idx_idempotency_records_expires_at ON idempotency_records (expires_at);
```

> **Migration path**: `V2` introduced `idempotency_keys` (single-key-per-endpoint model). `V4` added `idempotency_records` with composite `(idempotency_key, operation_key)` uniqueness. `V5` dropped `idempotency_keys`.

---

## 7. Version Number Model

```
environment_config.current_config_version  (one counter per environment)
        │
        │  +1 on each publish transaction
        ▼
config_version_history  (records which flags changed)
        │
        ├── flag_versions.config_version  (each flag snapshot tagged with same version)
        └── publish_jobs.config_version   (Outbox carries version number)
```

| Concept | Scope | Description |
|---------|-------|-------------|
| `config_version` | Per **environment** | SDK incremental sync watermark; `GET /snapshot?sinceVersion=N` |
| `flag_version` | Per **flag × environment** | Per-flag publish history sequence; used for rollback |
| `flag_versions.id` | Global | Snapshot row PK; referenced by `flag_environment_state.latest_version_id` |

**Publish transaction boundary** (single-flag publish):

1. `SELECT current_config_version FROM environment_config WHERE environment = :env FOR UPDATE`
2. `new_version = current + 1`
3. `INSERT flag_versions (... config_version = new_version ...)`
4. `UPDATE environment_config SET current_config_version = new_version`
5. `INSERT config_version_history (... changed_flag_ids = ARRAY[:flagId] ...)`
6. `INSERT publish_jobs (... status = 'pending' ...)`
7. `INSERT audit_events (...)`
8. `UPDATE flag_environment_state SET latest_version_id = ..., is_published = true, draft_dirty = false`
9. `COMMIT` → return `202 Accepted` + `configVersion`

---

## 8. JSONB Structure Conventions

### 8.1 `flag_rules.conditions` — Rule Conditions

```json
{
  "region": { "operator": "in", "values": ["US", "CA"] },
  "appVersion": { "operator": "semver_gte", "value": "3.2.0" },
  "userId": { "operator": "in", "values": ["usr_1", "usr_2"] },
  "segment": { "operator": "in_segment", "segmentId": "seg_gold_users" },
  "rolloutPercent": 5,
  "customAttributes": {
    "loyaltyTier": { "operator": "eq", "value": "gold" }
  }
}
```

| Field | Type | Description |
|-------|------|-------------|
| `region` | object | Geographic targeting |
| `appVersion` | object | Application version semver comparison |
| `userId` | object | User ID allowlist (small scale; large scale uses segment service) |
| `segment` | object | External user segment ID |
| `rolloutPercent` | number | 0–100, stable percentage rollout |
| `customAttributes` | object | Custom attribute matching |

**Allowed operators**: `eq`, `neq`, `in`, `not_in`, `semver_gte`, `semver_lte`, `in_segment`.

### 8.2 `flag_versions.snapshot` — Compiled Snapshot

```json
{
  "key": "checkout_v2",
  "type": "boolean",
  "defaultValue": false,
  "status": "published",
  "rolloutSalt": "a1b2c3d4e5f6",
  "rules": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "priority": 10,
      "conditions": { "region": { "operator": "in", "values": ["US", "CA"] }, "rolloutPercent": 5 },
      "value": true
    }
  ],
  "releaseId": "REL-2026-06-25-checkout",
  "killSwitchOverrides": []
}
```

### 8.3 `audit_events.diff` — Audit Diff

```json
{
  "before": { "status": "draft", "defaultValue": false },
  "after": { "status": "published", "defaultValue": false },
  "changedFields": ["status"]
}
```

### 8.4 `publish_jobs.payload` — Worker Payload

```json
{
  "applicationId": "550e8400-e29b-41d4-a716-446655440001",
  "applicationSlug": "checkout-service",
  "snapshot": { },
  "invalidateApps": ["checkout-service"]
}
```

---

## 9. Core Query Patterns and Index Mapping

| Access Scenario | Query Pattern | Primary Index |
|-----------------|---------------|---------------|
| Admin console flag list | `WHERE application_id = ? AND status = ?` | `idx_feature_flags_app_status` |
| Filter by tag | `JOIN feature_flag_tags` | `idx_feature_flag_tags_tag` |
| SDK incremental sync | `config_version_history WHERE environment = ? AND config_version > ?` | `uq_config_version_history_env_version` |
| Load flag rules (edit) | `flag_rules WHERE flag_id = ? AND environment = ? ORDER BY priority` | `idx_flag_rules_flag_env` |
| Explain replay | `flag_versions WHERE environment = ? AND config_version = ?` | `idx_flag_versions_env_config` |
| Active kill switch | `kill_switch_overrides WHERE environment = ? AND is_active` | `idx_kill_switch_active` |
| Worker Outbox consumption | `publish_jobs WHERE status IN ('pending','failed')` | `idx_publish_jobs_pending` |
| Audit query | `audit_events WHERE resource_type = ? AND created_at BETWEEN` | `idx_audit_events_resource` |

---

## 10. PostgreSQL vs Redis Responsibilities

| Data | PostgreSQL | Redis |
|------|------------|-------|
| Flag definitions and rule source data | ✅ Source of truth | ❌ |
| Compiled snapshots (hot path) | ✅ Historical archive (`flag_versions.snapshot`) | ✅ Current serving snapshot `fms:snap:{env}:{appId}:v{version}` |
| Current environment version | ✅ `environment_config` | ✅ Pointer key `fms:snap:{env}:{appId}:current` |
| Incremental deltas | ✅ `config_version_history` metadata | ✅ Optional precomputed deltas |
| Audit log | ✅ | ❌ |
| Explain results | ❌ Computed at runtime | ❌ |

---

## 11. Flyway Migration Inventory (As-built)

Actual migration files under `fms-server/src/main/resources/db/migration/`:

| Version | File | Contents |
|---------|------|----------|
| V1 | `V1__init_schema.sql` | `pgcrypto` extension; `environments`, `applications`, `feature_flags`, `environment_config`; seed data |
| V2 | `V2__management_schema.sql` | `tags`, `feature_flag_tags`, `releases`, `flag_rules`, `flag_versions`, `flag_environment_state`, `release_flags`, `publish_jobs`, `config_version_history`, `kill_switch_overrides`, `api_keys`, `audit_events`, legacy `idempotency_keys` |
| V3 | `V3__enum_columns_to_varchar.sql` | Convert status/type/action/scope columns to `VARCHAR(32)`; drop PostgreSQL ENUM types |
| V4 | `V4__idempotency_records.sql` | `idempotency_records` table (replaces `idempotency_keys` model) |
| V5 | `V5__schema_cleanup_and_indexes.sql` | Drop `idempotency_keys`; add GIN index on `flag_rules.conditions`; add `publish_jobs` env/flag indexes |

### 11.1 Extensions and `updated_at` Handling

```sql
CREATE EXTENSION IF NOT EXISTS "pgcrypto";  -- gen_random_uuid()
```

`updated_at` is **not** maintained by database triggers. JPA entities use `@PreUpdate` (`ApplicationEntity`, `FeatureFlagEntity`, `FlagRuleEntity`, `FlagEnvironmentStateEntity`); `EnvironmentConfigEntity` is updated explicitly in `PublishOrchestrator` on publish.

### 11.2 Planned Migrations (Not Yet Implemented)

The following were in the original design but are not present in V1–V5:

| Item | Purpose |
|------|---------|
| `idx_api_keys_active` | Partial index for non-revoked, non-expired keys |
| `idx_flag_versions_published_at` | Time-ordered version history queries |
| `idx_flag_versions_release` | Release-scoped version lookups |
| `set_updated_at()` trigger function | DB-level `updated_at` automation (superseded by JPA) |
| Monthly partitioning on `flag_versions` / `audit_events` | Phase 2 scale-out |

---

## 12. Capacity and Scale Estimates

| Metric | Initial Target | Table Impact |
|--------|----------------|--------------|
| Applications | 100+ | `applications` ~100 rows |
| Total flags | 5,000+ | `feature_flags` ~5k–50k rows |
| Rules | ~3 rules/flag | `flag_rules` ~15k–150k rows |
| Publish frequency | ~50/day/environment | `flag_versions` ~50k rows/year |
| Audit | Full recording | `audit_events` grows fastest; partitioning or archival required |

**`flag_versions` partitioning recommendation** (Phase 2): Monthly RANGE partition on `published_at`.

**`audit_events` partitioning recommendation**: Monthly partition on `created_at`, with automatic detach + archive.

---

## 13. Security and Compliance

| Requirement | Implementation |
|-------------|----------------|
| API keys not stored in plaintext | `api_keys.key_hash` + `key_prefix` |
| Tamper-evident audit | INSERT only; application service account has no UPDATE/DELETE |
| PII minimization | `audit_events` does not record full userId lists; `actor_ip_hash` replaces plaintext IP |
| Encryption in transit and at rest | PostgreSQL TDE / cloud RDS encryption; sensitive `conditions` may use application-layer AES-256-GCM |
| Backup | Daily full backup + continuous WAL archiving; RPO ≤ 5 min |

---

## 14. Open Questions

| # | Question | Affected Tables | Status |
|---|----------|-----------------|--------|
| 1 | Should large user segments be externalized to a Segment service? | `flag_rules.conditions.segment` | Open |
| 2 | `config_version_history` retention window: 100 vs 500? | Incremental sync compatibility | Open |
| 3 | Multi-region writes: single primary vs multi-primary conflict resolution? | `environment_config` write path | Open |
| 4 | Should `flag_versions` store compressed binary instead of JSONB? | Storage vs Explain query complexity | Open |
| 5 | Add planned partial/GIN indexes (`idx_api_keys_active`, `idx_flag_versions_*`)? | Query performance | Open — see §11.2 |

---

## 15. Appendix: Full CREATE TABLE SQL (MVP)

> Executable DDL summary aligned with Flyway V1–V5. Enum-like columns use `VARCHAR(32)`.

```sql
-- See §6 for full column definitions; core table CREATE skeleton below

CREATE TABLE environments (
    name         VARCHAR(32)  PRIMARY KEY,
    display_name VARCHAR(64)  NOT NULL,
    sort_order   SMALLINT     NOT NULL DEFAULT 0,
    is_production BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE applications (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug         VARCHAR(64)  NOT NULL UNIQUE,
    name         VARCHAR(128) NOT NULL,
    description  TEXT,
    status       VARCHAR(32)  NOT NULL DEFAULT 'active',
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
    type            VARCHAR(32) NOT NULL DEFAULT 'boolean',
    default_value   JSONB NOT NULL,
    status          VARCHAR(32) NOT NULL DEFAULT 'draft',
    rollout_salt    VARCHAR(64) NOT NULL,
    created_by      VARCHAR(256) NOT NULL,
    updated_by      VARCHAR(256),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_feature_flags_app_key UNIQUE (application_id, key)
);

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

CREATE TABLE environment_config (
    environment            VARCHAR(32) PRIMARY KEY REFERENCES environments(name),
    current_config_version BIGINT NOT NULL DEFAULT 0,
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE publish_jobs (
    id              BIGSERIAL PRIMARY KEY,
    flag_id         UUID NOT NULL REFERENCES feature_flags(id) ON DELETE RESTRICT,
    flag_key        VARCHAR(128) NOT NULL,
    environment     VARCHAR(32) NOT NULL REFERENCES environments(name),
    config_version  BIGINT NOT NULL,
    flag_version_id BIGINT REFERENCES flag_versions(id) ON DELETE RESTRICT,
    job_type        VARCHAR(32) NOT NULL DEFAULT 'publish',
    payload         JSONB NOT NULL,
    status          VARCHAR(32) NOT NULL DEFAULT 'pending',
    attempt_count   SMALLINT NOT NULL DEFAULT 0,
    max_attempts    SMALLINT NOT NULL DEFAULT 5,
    locked_by       VARCHAR(128),
    locked_at       TIMESTAMPTZ,
    next_retry_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    error_message   TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at    TIMESTAMPTZ
);

CREATE TABLE audit_events (
    id            BIGSERIAL PRIMARY KEY,
    actor         VARCHAR(256) NOT NULL,
    actor_ip_hash VARCHAR(64),
    action        VARCHAR(32) NOT NULL,
    resource_type VARCHAR(64) NOT NULL,
    resource_id   VARCHAR(256) NOT NULL,
    environment   VARCHAR(32),
    request_id    VARCHAR(64),
    diff          JSONB NOT NULL,
    metadata      JSONB NOT NULL DEFAULT '{}',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE idempotency_records (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key     VARCHAR(128) NOT NULL,
    operation_key       VARCHAR(512) NOT NULL,
    response_status     INT NOT NULL,
    response_body       JSONB NOT NULL,
    response_headers    JSONB,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at          TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_idempotency_key_operation UNIQUE (idempotency_key, operation_key)
);
```

---

## 16. References

- [Feature Management Service BRD](./Feature_Management_Service_BRD.md) — §10 Business Entities
- [Feature Management Service Technical Architecture](./Feature_Management_Service_Technical_Architecture.md) — §7 Caching, §11 Data Model
- [Feature Management Service Technology Stack](./Feature_Management_Service_Technology_Stack.md) — PostgreSQL / Flyway versions

---

*This document evolves with implementation. Schema changes must go through Flyway migration scripts in `fms-server/src/main/resources/db/migration/`; update this document's version number when migrations change.*
