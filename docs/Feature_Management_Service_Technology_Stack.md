# Feature Management Service — 技术栈与版本

| 属性 | 值 |
|------|-----|
| **文档版本** | 1.4 |
| **版本基线日期** | 2026-06-25 |
| **关联文档** | [技术架构](./Feature_Management_Service_Technical_Architecture.md) · [BRD](./Feature_Management_Service_BRD.md) |

---

## 1. 语言与构建

| 技术 | 版本 |
|------|------|
| JDK | 25.0.3 (LTS)，最低 17 |
| Gradle | 9.6.0 |
| Spring Boot Gradle Plugin | 4.1.0 |
| Kotlin（可选） | 2.3.21 |

## 2. 应用框架

| 技术 | 版本 |
|------|------|
| Spring Boot | 4.1.0 |
| Spring Framework | 7.0.8 |
| Spring Security | 7.1.0 |
| Spring Session | 4.1.0 |
| Spring Data BOM | 2026.0.0 |
| Tomcat（嵌入式） | 11.0.22 |

## 3. Web 与 API

| 技术 | 版本 |
|------|------|
| Spring Web MVC | Boot 4.1.0 |
| Server-Sent Events (SSE) | Spring MVC SSE，`GET /v1/sync/stream` |
| Spring WebFlux（可选，SSE 高并发） | Boot 4.1.0 |
| Jackson 3 BOM | 3.1.4 |
| Jackson 2 BOM | 2.21.4 |
| Hibernate Validator | 9.1.0.Final |
| springdoc-openapi | 3.0.3 |
| OpenAPI | 3.1 |
| Swagger UI | 5.32.2 |
| Spring HATEOAS（可选） | 3.1.1 |
| Reactor BOM | 2025.0.6 |

## 4. 数据持久化

| 技术 | 版本 |
|------|------|
| PostgreSQL | 18.4 |
| PostgreSQL JDBC | 42.7.11 |
| Hibernate ORM | 7.4.1.Final |
| HikariCP | 7.0.2 |
| Spring Data JPA | Boot 4.1.0 |
| Flyway | 12.4.0 |
| Liquibase（备选） | 5.0.3 |

## 5. 缓存

| 技术 | 版本 |
|------|------|
| Redis | 8.8.0 |
| Valkey（备选） | 9.1.0 |
| Lettuce | 7.5.2.RELEASE |
| Spring Data Redis | Boot 4.1.0 |
| Caffeine（可选） | 3.2.4 |

## 6. 异步发布

| 技术 | 版本 |
|------|------|
| PostgreSQL Outbox（`publish_jobs`） | PostgreSQL 18.4 |
| Spring `@Scheduled` + Spring Data JPA | Boot 4.1.0 |
| Redis Pub/Sub（变更通知） | Redis 8.8.0 |

## 7. 规则引擎

| 技术 | 版本 |
|------|------|
| fms-rule-engine（Java 库） | Java 21+ |

## 8. 安全

| 技术 | 版本 |
|------|------|
| TLS | 1.2+（推荐 1.3） |
| OAuth2 / OIDC | Spring Security 7.1.0 |
| API Key / mTLS | — |
| Nimbus JOSE + JWT | Spring Security 内置 |

## 9. 可观测性

| 技术 | 版本 |
|------|------|
| Micrometer | 1.17.0 |
| Micrometer Tracing | 1.7.0 |
| OpenTelemetry | 1.62.0 |
| Logback | 1.5.34 |
| Spring Boot Actuator | Boot 4.1.0 |

## 10. 前端（管理控制台）

| 技术 | 版本 |
|------|------|
| Vaadin Platform | 25.1.7 |
| Vaadin Spring Boot Starter | 25.1.7 |

## 11. 客户端 SDK

| SDK | 语言 / 运行时 | 版本基线 |
|-----|--------------|----------|
| fms-sdk-java | Java | 17+，随服务发版 |
| fms-sdk-go | Go | 1.24+ |
| fms-sdk-node | Node.js | 22 LTS |
| fms-sdk-web | TypeScript | 5.x |
| fms-sdk-mobile | Kotlin / Swift | 独立发版 |

| SDK 依赖（Java） | 版本 |
|------------------|------|
| Jackson | 3.1.4 |
| Java HttpClient | JDK 17+ |
| Micrometer（可选） | 1.17.0 |

## 12. 测试

| 技术 | 版本 |
|------|------|
| JUnit Jupiter | 6.0.3 |
| Mockito | 5.23.0 |
| Spring Boot Test | Boot 4.1.0 |
| Testcontainers | 2.0.5 |
| Testcontainers Redis | 2.2.4 |
| Testcontainers PostgreSQL | 2.0.5 |
| Spring REST Docs | 4.0.1 |
| JaCoCo | 0.8.x |

## 13. 部署与基础设施

| 技术 | 版本 |
|------|------|
| 容器基础镜像（JRE） | eclipse-temurin:25-jre-alpine |
| 编排 | Kubernetes (EKS) |
| 镜像构建 | Spring Boot Buildpacks / Jib |
