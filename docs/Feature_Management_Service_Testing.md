# Feature Management Service — Testing Guide

| Attribute | Value |
|-----------|-------|
| **Document version** | 1.0 |
| **Last updated** | 2026-06-30 |
| **Related docs** | [Technical Architecture](./Feature_Management_Service_Technical_Architecture.md) · [API Design](./Feature_Management_Service_API_Design.md) |

---

## 1. Overview

The project uses **JUnit 5, Spring Boot Test, Mockito, and Testcontainers** to cover rule-engine unit tests, service-layer unit tests, and full-stack API integration tests via MockMvc.

| Module | Tests (approx.) | Description |
|--------|-----------------|-------------|
| `fms-rule-engine` | 10 | Pure unit tests, no Spring context |
| `fms-server` | 81 | Unit + integration tests |
| `fms-common` | 0 | No tests yet |
| **Total** | **91** | All passing on last full run |

---

## 2. Prerequisites

| Dependency | Purpose |
|------------|---------|
| **JDK 25** | Matches the Gradle toolchain |
| **Docker Desktop** | Integration tests start Postgres 18 / Redis 8 via Testcontainers |
| **Gradle** | Use the project wrapper: `gradlew` / `gradlew.bat` |

> Integration tests do **not** require manually starting Postgres/Redis from `docker compose`. Testcontainers launches dedicated containers automatically. Docker must be running locally.

---

## 3. Running tests

### 3.1 Full suite

```bash
# Windows
gradlew.bat test

# macOS / Linux
./gradlew test
```

### 3.2 Force re-run (ignore cache)

```bash
./gradlew test --rerun-tasks
```

### 3.3 Single module / class / method

```bash
# fms-server only
./gradlew :fms-server:test

# Specific test class
./gradlew :fms-server:test --tests "com.fms.explain.ExplainApiIntegrationTest"

# Specific test method
./gradlew :fms-server:test --tests "com.fms.sync.SyncApiIntegrationTest.snapshotReturnsFullPayloadAfterPublish"
```

### 3.4 Test reports

After execution, open the HTML report at:

```
fms-server/build/reports/tests/test/index.html
```

---

## 4. Test layers

```
┌─────────────────────────────────────────────────────────────┐
│  API integration tests (MockMvc + Testcontainers)           │
│  ExplainApiIntegrationTest, SyncApiIntegrationTest, ...     │
├─────────────────────────────────────────────────────────────┤
│  Integration test base classes                              │
│  IntegrationTestSupport → SyncIntegrationTestSupport → ...  │
├─────────────────────────────────────────────────────────────┤
│  Service unit tests (Mockito / @MockBean)                   │
│  ExplainServiceTest, SyncServiceTest, PublishOrchestratorTest│
├─────────────────────────────────────────────────────────────┤
│  Rule engine unit tests (plain Java)                        │
│  DefaultRuleEngineTest (fms-rule-engine)                    │
└─────────────────────────────────────────────────────────────┘
```

### 4.1 Unit tests

No Spring context, or a minimal slice (e.g. `@ExtendWith(MockitoExtension.class)`).

| Test class | Subject under test | Key scenarios |
|------------|-------------------|---------------|
| `DefaultRuleEngineTest` | `DefaultRuleEngine` | Rule matching, percentage rollout, archived / not published |
| `ExplainServiceTest` | `ExplainService` | Decision trace, replay, kill switch |
| `EvaluateServiceTest` | `EvaluateService` | Single-flag and batch evaluation |
| `SyncServiceTest` | `SyncService` | Snapshot, delta, ETag |
| `PublishOrchestratorTest` | `PublishOrchestrator` | Publish orchestration |
| `PublishWorkerServiceTest` | `PublishWorkerService` | Worker retry and failure handling |
| `ApiKeyAuthenticationServiceTest` | `ApiKeyAuthenticationService` | API key hash validation |
| `FmsMetricsTest` / `FmsMetricsRecordingTest` | `FmsMetrics` | Metric registration |
| `RequestContextFilterTest` | `RequestContextFilter` | Request context / MDC |
| `ActorIpHasherTest` | `ActorIpHasher` | IP hashing |

### 4.2 API integration tests

Boot the full Spring Boot context, send HTTP requests via **MockMvc**, and use **Testcontainers** for PostgreSQL and Redis.

| Test class | API domain | Tests |
|------------|------------|-------|
| `ManagementApiIntegrationTest` | Management API | 8 |
| `SyncApiIntegrationTest` | Config Sync API | 13 |
| `EvaluateApiIntegrationTest` | Evaluation API | 8 |
| `ExplainApiIntegrationTest` | Explain API | 7 |
| `ApiKeyAuthenticationIntegrationTest` | Data-plane auth | 7 |
| `IdempotencyIntegrationTest` | Idempotency | 2 |
| `PublishInProgressIntegrationTest` | Concurrent publish | 2 |
| `PlatformControllerTest` | Health / Ready / OpenAPI | 3 |
| `FmsApplicationTests` | Context load | 1 |

---

## 5. Integration test infrastructure

### 5.1 Class hierarchy

```
ManagementIntegrationTestSupport     ← HTTP helpers (createFlag, publishFlag, …)
        ↑
IntegrationTestSupport               ← @SpringBootTest, Testcontainers, Redis reset
        ↑
SyncIntegrationTestSupport           ← publish worker, API key, data-plane auth
        ↑
EvaluateIntegrationTestSupport       ← shared API key for evaluate/explain tests
        ↑
ExplainApiIntegrationTest / EvaluateApiIntegrationTest / …
```

### 5.2 IntegrationTestContainers

Path: `fms-server/src/test/java/com/fms/testsupport/IntegrationTestContainers.java`

- **One shared** Postgres 18 + Redis 8 pair for the entire module
- Lazy, singleton startup per JVM to avoid duplicate containers and resource contention
- Registers `spring.datasource.*` and `spring.data.redis.*` via `@DynamicPropertySource`

### 5.3 IntegrationTestSupport

Path: `fms-server/src/test/java/com/fms/testsupport/IntegrationTestSupport.java`

| Setting | Value |
|---------|-------|
| Profile | `local` |
| Web layer | `@AutoConfigureMockMvc` |
| Database | Flyway migrations (same as production) |

**Redis reset (`resetRedis()`):**

Redis is flushed before each test method. Because the app starts a `RedisMessageListenerContainer` (long-lived pub/sub connections), cleanup:

1. Stops the listener
2. Runs `FLUSHDB`
3. Restarts the listener

This prevents pub/sub connections and `FLUSHDB` from competing for the connection pool (`RedisCommandTimeoutException`).

### 5.4 SyncIntegrationTestSupport

Each `@BeforeEach` also:

| Step | Purpose |
|------|---------|
| `resetRedis()` | Isolate Redis cache state |
| `publishWorkerService.processPendingJobs()` | Drain leftover publish jobs (avoids 409 conflicts) |
| Create a data-plane API key | Authenticate sync/evaluate/explain requests |

**Helper methods:**

| Method | Description |
|--------|-------------|
| `publishAndProcess(appId, flagKey, env)` | Publish a flag, run the worker synchronously, return `configVersion` |
| `withDataPlaneAuth(builder)` | Add `Authorization: ApiKey …` to a MockMvc request |
| `uniqueKey(prefix)` / `uniqueSlug(prefix)` | Generate unique flag/app names to avoid collisions |

**Test-only properties:**

```properties
fms.worker.publish.poll-interval-ms=60000   # disable background polling during tests
fms.sync.delta-max-gap=5                    # smaller delta gap for easier testing
```

### 5.5 Gradle test configuration

`fms-server/build.gradle.kts`:

```kotlin
tasks.named<Test>("test") {
    maxParallelForks = 1   // serial execution; avoids Testcontainers contention
}
```

---

## 6. Writing new integration tests

### 6.1 Choose a base class

| Scenario | Recommended base |
|----------|------------------|
| Context load only | `IntegrationTestContainers.register()` + `@SpringBootTest` |
| Management API | `SyncIntegrationTestSupport` |
| Sync / Evaluate / Explain API | `SyncIntegrationTestSupport` or `EvaluateIntegrationTestSupport` |
| Enforced API key auth | Extend `SyncIntegrationTestSupport` and add `@TestPropertySource(properties = "fms.security.api-key.enforced=true")` |

### 6.2 Data-plane request template

```java
mockMvc.perform(withDataPlaneAuth(get("/api/v1/sync/snapshot"))
                .param("environment", "dev")
                .param("appId", SEED_APP))
        .andExpect(status().isOk());
```

Evaluate / Explain tests use the `apiKeyAuthorization` constant from `EvaluateIntegrationTestSupport`.

### 6.3 Tests involving publish

```java
// Single publish + worker
long version = publishAndProcess(SEED_APP, flagKey, "dev");

// Multiple publishes in one test — process the worker after each publish
publishFlag(SEED_APP, flagKey, "dev").andExpect(status().isAccepted());
publishWorkerService.processPendingJobs();
publishFlag(SEED_APP, flagKey, "dev").andExpect(status().isAccepted());
publishWorkerService.processPendingJobs();
```

### 6.4 Naming and isolation conventions

- Use `uniqueKey("prefix")` for flag keys to avoid collisions in the shared database
- Seed app `checkout-service` (`SEED_APP`) is created by Flyway migrations
- Do not rely on absolute `configVersion` values; assert relative relationships or use the return value of `publishAndProcess`

---

## 7. Test profile and authentication

Integration tests use Spring profile **`local`** (`application-local.yml`):

| Setting | Test behavior |
|---------|---------------|
| `fms.security.oauth2.enabled=false` | OIDC disabled |
| `fms.security.api-key.enforced=false` (default) | Management API unauthenticated; data-plane can still use API keys |
| `@TestPropertySource(enforced=true)` | Used by `ApiKeyAuthenticationIntegrationTest` for enforced-auth scenarios |

> Production defaults to `fms.security.api-key.enforced=true`. Sync/Evaluate/Explain integration tests send API keys via `withDataPlaneAuth()`, matching production behavior.

---

## 8. Troubleshooting

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| `RedisCommandTimeoutException` | Multiple containers / pub/sub vs `FLUSHDB` conflict | Extend `IntegrationTestSupport` and use shared `IntegrationTestContainers` |
| `401 UNAUTHORIZED` on sync/evaluate | Missing API key when `enforced=true` | Use `withDataPlaneAuth()` or `apiKeyAuthorization` |
| `409` on publish | Previous publish job not processed | Call `publishWorkerService.processPendingJobs()` |
| Testcontainers fails to start | Docker not running | Start Docker Desktop |
| `InvalidDefinitionException: Instant` | Test `ObjectMapper` missing JSR310 | Use `objectMapper` from `ManagementIntegrationTestSupport` (registers `JavaTimeModule`) |
| Tests pass but Gradle shows UP-TO-DATE | Build cache | Run with `--rerun-tasks` |

---

## 9. Directory layout

```
fms-server/src/test/java/com/fms/
├── testsupport/
│   ├── IntegrationTestContainers.java    # shared Testcontainers
│   └── IntegrationTestSupport.java       # integration test base
├── management/
│   ├── ManagementIntegrationTestSupport.java
│   ├── ManagementApiIntegrationTest.java
│   ├── IdempotencyIntegrationTest.java
│   └── PublishInProgressIntegrationTest.java
├── sync/
│   ├── SyncIntegrationTestSupport.java
│   ├── SyncApiIntegrationTest.java
│   └── service/SyncServiceTest.java
├── evaluate/
│   ├── EvaluateIntegrationTestSupport.java
│   ├── EvaluateApiIntegrationTest.java
│   └── service/EvaluateServiceTest.java
├── explain/
│   ├── ExplainApiIntegrationTest.java
│   └── service/ExplainServiceTest.java
├── security/
│   ├── ApiKeyAuthenticationIntegrationTest.java
│   └── ApiKeyAuthenticationServiceTest.java
├── platform/PlatformControllerTest.java
├── worker/PublishWorkerServiceTest.java
├── observability/...
└── FmsApplicationTests.java

fms-rule-engine/src/test/java/com/fms/ruleengine/
└── DefaultRuleEngineTest.java
```

---

## 10. CI recommendations

```bash
# Minimal CI command
./gradlew test --no-daemon

# Requires Docker-in-Docker or a mounted Docker socket
# Recommended settings:
#   - maxParallelForks = 1 (already configured)
#   - timeout: full integration suite ~2–3 minutes
```

Optional enhancements:

- Upload `fms-server/build/reports/tests/test/` as a CI artifact
- Require `./gradlew test` to pass on pull requests
- Add JaCoCo coverage reporting when needed
