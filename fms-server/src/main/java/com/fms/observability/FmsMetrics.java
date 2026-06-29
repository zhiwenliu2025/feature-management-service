package com.fms.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class FmsMetrics {

    private final MeterRegistry meterRegistry;
    private final Counter evalRequests;
    private final Timer evalLatency;
    private final Timer ruleEvaluationDuration;
    private final Counter syncRequests;
    private final Counter redisCacheHits;
    private final Counter redisCacheMisses;
    private final Counter errors;
    private final Counter killSwitchActivations;
    private final Counter explainRequests;
    private final Timer publishDuration;
    private final AtomicLong cacheHitsTotal = new AtomicLong();
    private final AtomicLong cacheMissesTotal = new AtomicLong();
    private final AtomicLong publishBacklog = new AtomicLong();
    private final Map<String, AtomicLong> configVersionLagByEnvironment = new ConcurrentHashMap<>();

    public FmsMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.evalRequests = Counter.builder("fms.eval.requests")
                .description("Total remote evaluation requests")
                .register(meterRegistry);
        this.evalLatency = Timer.builder("fms.eval.latency")
                .description("Remote evaluation latency")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
        this.ruleEvaluationDuration = Timer.builder("fms.rule.evaluation.duration")
                .description("Rule engine evaluation duration")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
        this.syncRequests = Counter.builder("fms.sync.requests")
                .description("Config sync snapshot requests")
                .register(meterRegistry);
        this.redisCacheHits = Counter.builder("fms.redis.cache.hits")
                .description("Redis L2 cache hits")
                .register(meterRegistry);
        this.redisCacheMisses = Counter.builder("fms.redis.cache.misses")
                .description("Redis L2 cache misses")
                .register(meterRegistry);
        this.errors = Counter.builder("fms.errors")
                .description("Application errors by code and module")
                .register(meterRegistry);
        this.killSwitchActivations = Counter.builder("fms.kill.switch.activations")
                .description("Kill switch activations")
                .register(meterRegistry);
        this.explainRequests = Counter.builder("fms.explain.requests")
                .description("Explain API requests")
                .register(meterRegistry);
        this.publishDuration = Timer.builder("fms.publish.duration")
                .description("Publish worker job processing duration")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);

        Gauge.builder("fms.cache.hit.ratio", cacheHitsTotal, hits -> ratio(hits.get(), cacheMissesTotal.get()))
                .description("Redis L2 cache hit ratio")
                .register(meterRegistry);
        Gauge.builder("fms.publish.jobs.backlog", publishBacklog, AtomicLong::get)
                .description("Pending and failed publish jobs")
                .register(meterRegistry);
        registerLagGauges(meterRegistry);
    }

    private void registerLagGauges(MeterRegistry meterRegistry) {
        for (String environment : new String[] {"dev", "staging", "prod"}) {
            AtomicLong lagSeconds = configVersionLagByEnvironment.computeIfAbsent(
                    environment, ignored -> new AtomicLong(0));
            Gauge.builder("fms.config.version.lag", lagSeconds, AtomicLong::get)
                    .description("Config propagation lag in seconds")
                    .tag("environment", environment)
                    .register(meterRegistry);
        }
    }

    public void recordEvaluation(
            String appId,
            String flagKey,
            String reasonCode,
            long durationNanos,
            long ruleEvalNanos) {
        evalRequests.increment();
        evalLatency.record(durationNanos, TimeUnit.NANOSECONDS);
        ruleEvaluationDuration.record(ruleEvalNanos, TimeUnit.NANOSECONDS);
        meterRegistry.counter(
                "fms.eval.requests.by.flag",
                "app_id", safeTag(appId),
                "flag_key", safeTag(flagKey),
                "reason_code", safeTag(reasonCode))
                .increment();
    }

    public void recordBatchEvaluation(String appId, int flagCount, long durationNanos) {
        evalRequests.increment();
        evalLatency.record(durationNanos, TimeUnit.NANOSECONDS);
        meterRegistry.counter("fms.eval.batch.requests", "app_id", safeTag(appId))
                .increment(flagCount);
    }

    public void recordSyncRequest(String syncType, String cacheTier, String outcome) {
        syncRequests.increment();
        meterRegistry.counter(
                "fms.sync.requests.detailed",
                "sync_type", safeTag(syncType),
                "cache_tier", safeTag(cacheTier),
                "outcome", safeTag(outcome))
                .increment();
    }

    public void recordRedisCacheHit(String keyType) {
        redisCacheHits.increment();
        cacheHitsTotal.incrementAndGet();
        meterRegistry.counter("fms.redis.cache.hits.detailed", "key_type", safeTag(keyType))
                .increment();
    }

    public void recordRedisCacheMiss(String keyType) {
        redisCacheMisses.increment();
        cacheMissesTotal.incrementAndGet();
        meterRegistry.counter("fms.redis.cache.misses.detailed", "key_type", safeTag(keyType))
                .increment();
    }

    public void recordError(String errorCode, String module) {
        errors.increment();
        meterRegistry.counter(
                "fms.errors.detailed",
                "error_code", safeTag(errorCode),
                "module", safeTag(module))
                .increment();
    }

    public void recordKillSwitchActivation(String environment, String scope) {
        killSwitchActivations.increment();
        meterRegistry.counter(
                "fms.kill.switch.activations.detailed",
                "environment", safeTag(environment),
                "scope", safeTag(scope))
                .increment();
    }

    public void recordExplainRequest(String mode) {
        explainRequests.increment();
        meterRegistry.counter("fms.explain.requests.detailed", "mode", safeTag(mode))
                .increment();
    }

    public void recordPublishDuration(long durationNanos, String environment, String outcome) {
        publishDuration.record(durationNanos, TimeUnit.NANOSECONDS);
        meterRegistry.timer(
                "fms.publish.duration.detailed",
                "environment", safeTag(environment),
                "outcome", safeTag(outcome))
                .record(durationNanos, TimeUnit.NANOSECONDS);
    }

    public void setPublishBacklog(long backlog) {
        publishBacklog.set(backlog);
    }

    public void setConfigVersionLag(String environment, long lagSeconds) {
        configVersionLagByEnvironment
                .computeIfAbsent(environment, ignored -> new AtomicLong())
                .set(lagSeconds);
    }

    private static double ratio(long hits, long misses) {
        long total = hits + misses;
        return total == 0 ? 1.0 : (double) hits / total;
    }

    static String safeTag(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.length() > 64 ? value.substring(0, 64) : value;
    }
}
