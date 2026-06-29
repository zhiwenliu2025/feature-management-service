package com.fms.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FmsMetricsRecordingTest {

    private SimpleMeterRegistry registry;
    private FmsMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new FmsMetrics(registry);
    }

    @Test
    void recordsEvaluationAndCacheMetrics() {
        metrics.recordEvaluation("checkout-service", "checkout_v2", "RULE_MATCH", 5_000_000L, 2_000_000L);
        metrics.recordRedisCacheHit("snapshot");
        metrics.recordRedisCacheMiss("delta");
        metrics.recordError("FLAG_NOT_FOUND", "evaluate");
        metrics.setPublishBacklog(3);
        metrics.setConfigVersionLag("prod", 42);

        assertEquals(1.0, registry.get("fms.eval.requests").counter().count());
        assertEquals(1.0, registry.get("fms.redis.cache.hits").counter().count());
        assertEquals(1.0, registry.get("fms.redis.cache.misses").counter().count());
        assertEquals(1.0, registry.get("fms.errors").counter().count());
        assertEquals(3.0, registry.get("fms.publish.jobs.backlog").gauge().value());
        assertEquals(42.0, registry.get("fms.config.version.lag").tag("environment", "prod").gauge().value());
        assertTrue(registry.get("fms.cache.hit.ratio").gauge().value() > 0.0);
    }
}
