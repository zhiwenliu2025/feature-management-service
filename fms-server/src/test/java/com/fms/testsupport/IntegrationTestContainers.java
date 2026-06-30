package com.fms.testsupport;

import com.redis.testcontainers.RedisContainer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared Postgres/Redis containers for all integration tests in this module.
 * Started once per JVM to avoid duplicate containers and Redis connection exhaustion.
 */
public final class IntegrationTestContainers {

    private static final Object START_LOCK = new Object();

    private static PostgreSQLContainer postgres;
    private static RedisContainer redis;

    private IntegrationTestContainers() {
    }

    public static void register(DynamicPropertyRegistry registry) {
        ensureStarted();
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    private static void ensureStarted() {
        if (postgres != null) {
            return;
        }
        synchronized (START_LOCK) {
            if (postgres != null) {
                return;
            }
            postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:18"));
            redis = new RedisContainer(DockerImageName.parse("redis:8"));
            postgres.start();
            redis.start();
        }
    }
}
