# Feature Management Service (FMS)

Centralized feature-flag platform for managing thousands of flags across web, API, and mobile clients. FMS provides lifecycle management, low-latency evaluation, config sync for SDKs, and explainability for safe progressive delivery and incident response.

## Capabilities

| Area | Description |
|------|-------------|
| **Management API** | CRUD for flags, rules, environments, releases; publish, rollback, promote; kill switch; audit log |
| **Config Sync API** | Full/incremental snapshots and SSE version streaming for client SDKs |
| **Evaluation API** | Stateless remote flag evaluation (single and batch) |
| **Explain API** | Structured decision traces for support and debugging |
| **Rule engine** | Deterministic evaluation with percentage rollout, targeting, and short-circuit rules |
| **Publish pipeline** | PostgreSQL outbox (`publish_jobs`) → worker → Redis compiled snapshots |

The admin console (Vaadin 25, Aura theme) is documented but not yet implemented in this repository.

## Architecture

```
Control plane:  Admin / CI ──► Management API ──► PostgreSQL
                                      │
                                      ▼
                               Publish Worker ──► Redis

Data plane:     Client SDK ◄──► Sync API ◄──► Redis
                    │
                    ├── local snapshot evaluation (primary path)
                    ├── Evaluation API (fallback / server-only)
                    └── Explain API (debug / support)
```

See [Technical Architecture](docs/Feature_Management_Service_Technical_Architecture.md) for the full design.

## Repository layout

| Module | Description |
|--------|-------------|
| `fms-server` | Spring Boot application — all HTTP APIs, persistence, cache, publish worker |
| `fms-rule-engine` | Shared deterministic rule engine library |
| `fms-common` | Shared DTOs, exceptions, API types |
| `docs/` | BRD, API design, database schema, UI design, technology stack |
| `observability/` | Prometheus alert rules and Grafana dashboard templates |

## Prerequisites

- **JDK 25** (toolchain configured in Gradle; minimum Java 17 for SDK consumers)
- **Docker** (for local PostgreSQL and Redis via Compose)
- **Gradle 9.6+** (wrapper included: `./gradlew`)

## Quick start

### 1. Start infrastructure

```bash
docker compose up -d
```

This starts:

| Service | Port | Credentials |
|---------|------|-------------|
| PostgreSQL 18 | `5432` | db `fms`, user/password `fms` |
| Redis 8 | `6379` | no password |
| pgAdmin (optional) | `5050` | `admin@local.dev` / `admin` |

### 2. Configure environment (optional)

```bash
cp .env.example .env
```

Defaults match `docker-compose.yml`. For local development, use the `local` profile (disables OAuth2 and API-key enforcement for easier testing).

### 3. Run the server

**Windows:**

```bash
gradlew.bat :fms-server:bootRun --args="--spring.profiles.active=local"
```

**macOS / Linux:**

```bash
./gradlew :fms-server:bootRun --args='--spring.profiles.active=local'
```

The server listens on **http://localhost:8080** by default.

### 4. Verify

```bash
curl http://localhost:8080/api/health
curl http://localhost:8080/api/ready
```

Open **http://localhost:8080/swagger-ui.html** for interactive API docs.

## API surface

| Module | Base path | Auth |
|--------|-----------|------|
| Platform | `/api/health`, `/api/ready`, `/api/v1/openapi.json` | None |
| Management | `/api/v1/management` | OAuth2 OIDC + RBAC |
| Config Sync | `/api/v1/sync` | API key / mTLS |
| Evaluation | `/api/v1/evaluate` | API key / mTLS |
| Explain | `/api/v1/explain` | API key + `explain:read` |

Full contract: [API Design](docs/Feature_Management_Service_API_Design.md) · live OpenAPI at `/api/v1/openapi.json`

### RBAC roles (management plane)

| Role | Capabilities |
|------|--------------|
| `viewer` | Read flags, audit |
| `editor` | Create/update flags and rules (draft) |
| `publisher` | Publish, rollback, promote |
| `kill_switch` | Emergency disable |
| `admin` | Applications, API keys, all operations |

## Configuration

Key environment variables (see [`.env.example`](.env.example)):

| Variable | Default | Description |
|----------|---------|-------------|
| `FMS_DB_HOST` | `localhost` | PostgreSQL host |
| `FMS_DB_PORT` | `5432` | PostgreSQL port |
| `FMS_DB_NAME` | `fms` | Database name |
| `FMS_DB_USER` | `fms` | Database user |
| `FMS_DB_PASSWORD` | `fms` | Database password |
| `FMS_REDIS_HOST` | `localhost` | Redis host |
| `FMS_REDIS_PORT` | `6379` | Redis port |
| `FMS_SERVER_PORT` | `8080` | HTTP port |
| `FMS_OIDC_ISSUER_URI` | — | OIDC issuer (required when OAuth2 enabled) |
| `SPRING_PROFILES_ACTIVE` | — | Set to `local` for relaxed local security |

Production settings are in `fms-server/src/main/resources/application.yml`. Profile-specific overrides: `application-local.yml`.

## Build and test

```bash
# Build all modules
./gradlew build

# Run unit and integration tests (Testcontainers — Docker required)
./gradlew test

# Build runnable JAR
./gradlew :fms-server:bootJar
java -jar fms-server/build/libs/fms-server-0.1.0-SNAPSHOT.jar
```

Integration tests spin up PostgreSQL and Redis via Testcontainers automatically.

## Observability

Actuator endpoints (when authorized):

- `/actuator/health`
- `/actuator/metrics`
- `/actuator/prometheus`

OpenTelemetry tracing is configurable via `OTEL_EXPORTER_OTLP_ENDPOINT`. Sample Prometheus alerts and a Grafana dashboard live under [`observability/`](observability/).

## Technology stack

| Layer | Technology |
|-------|------------|
| Runtime | Java 25, Spring Boot 4.1 |
| Persistence | PostgreSQL 18, Flyway, Spring Data JPA |
| Cache | Redis 8, Lettuce |
| API docs | springdoc-openapi 3.x, OpenAPI 3.1 |
| Security | Spring Security, OAuth2 resource server, API keys |
| Admin UI (planned) | Vaadin 25.1, Aura theme |

Details: [Technology Stack](docs/Feature_Management_Service_Technology_Stack.md)

## Documentation

| Document | Description |
|----------|-------------|
| [BRD](docs/Feature_Management_Service_BRD.md) | Business requirements and user stories |
| [Technical Architecture](docs/Feature_Management_Service_Technical_Architecture.md) | System design, control vs data plane, caching |
| [API Design](docs/Feature_Management_Service_API_Design.md) | HTTP API modules, schemas, errors |
| [Database Schema](docs/Feature_Management_Service_Database_Schema.md) | PostgreSQL tables and migrations |
| [Redis Cache Design](docs/Feature_Management_Service_Redis_Cache_Design.md) | Snapshot keys and invalidation |
| [UI Design](docs/Feature_Management_Service_UI_Design.md) | Admin console (Vaadin Aura) |
| [Technology Stack](docs/Feature_Management_Service_Technology_Stack.md) | Versions and dependencies |

## License

Internal enterprise project. No public license is defined in this repository.
