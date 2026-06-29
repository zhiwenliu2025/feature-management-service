package com.fms.evaluate.service;

import com.fms.cache.SnapshotCacheService;
import com.fms.common.exception.FmsErrorCode;
import com.fms.common.exception.FmsException;
import com.fms.evaluate.dto.BatchEvaluateRequest;
import com.fms.evaluate.dto.BatchEvaluateResponse;
import com.fms.evaluate.dto.BatchFlagResult;
import com.fms.evaluate.dto.EvaluateRequest;
import com.fms.evaluate.dto.EvaluateResponse;
import com.fms.observability.FmsMetrics;
import com.fms.repository.FeatureFlagRepository;
import com.fms.ruleengine.EvaluationContext;
import com.fms.ruleengine.EvaluationResult;
import com.fms.ruleengine.ReasonCode;
import com.fms.ruleengine.RuleEngine;
import com.fms.sync.dto.SnapshotResponse;
import com.fms.sync.service.SnapshotLoaderService;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class EvaluateService {

    private final RuleEngine ruleEngine;
    private final SnapshotCacheService snapshotCacheService;
    private final SnapshotLoaderService snapshotLoaderService;
    private final FeatureFlagRepository featureFlagRepository;
    private final FmsMetrics metrics;

    public EvaluateService(
            RuleEngine ruleEngine,
            SnapshotCacheService snapshotCacheService,
            SnapshotLoaderService snapshotLoaderService,
            FeatureFlagRepository featureFlagRepository,
            FmsMetrics metrics) {
        this.ruleEngine = ruleEngine;
        this.snapshotCacheService = snapshotCacheService;
        this.snapshotLoaderService = snapshotLoaderService;
        this.featureFlagRepository = featureFlagRepository;
        this.metrics = metrics;
    }

    @Observed(name = "fms.evaluate", contextualName = "evaluate-flag")
    public EvaluateResponse evaluate(String flagKey, EvaluateRequest request) {
        long start = System.nanoTime();
        SnapshotResponse snapshot = resolveSnapshot(request.environment(), request.appId(), request.configVersion());
        EvaluationContext context = toContext(request.context());
        long ruleStart = System.nanoTime();
        BatchFlagResult result = evaluateInSnapshot(flagKey, snapshot, context, true);
        long ruleEvalNanos = System.nanoTime() - ruleStart;
        long durationNanos = System.nanoTime() - start;

        metrics.recordEvaluation(
                request.appId(), flagKey, result.reasonCode(), durationNanos, ruleEvalNanos);

        SnapshotResponse.FlagSnapshot flag = findFlag(snapshot, flagKey).orElse(null);
        String type = flag == null ? "boolean" : flag.type();
        return new EvaluateResponse(
                result.flagKey(),
                result.value(),
                result.enabled(),
                type,
                snapshot.configVersion(),
                "remote",
                result.reasonCode(),
                durationNanos / 1_000_000);
    }

    @Observed(name = "fms.evaluate.batch", contextualName = "evaluate-batch")
    public BatchEvaluateResponse evaluateBatch(BatchEvaluateRequest request) {
        long start = System.nanoTime();
        SnapshotResponse snapshot = resolveSnapshot(request.environment(), request.appId(), null);
        EvaluationContext context = toContext(request.context());

        List<BatchFlagResult> results = request.flagKeys().stream()
                .map(flagKey -> evaluateInSnapshot(flagKey, snapshot, context, false))
                .toList();

        long durationNanos = System.nanoTime() - start;
        metrics.recordBatchEvaluation(request.appId(), request.flagKeys().size(), durationNanos);
        long latencyMs = durationNanos / 1_000_000;
        return new BatchEvaluateResponse(snapshot.configVersion(), "remote", results, latencyMs);
    }

    private BatchFlagResult evaluateInSnapshot(
            String flagKey,
            SnapshotResponse snapshot,
            EvaluationContext context,
            boolean failIfMissing) {
        if (snapshot.deletedFlagKeys().contains(flagKey)) {
            return new BatchFlagResult(flagKey, false, false, ReasonCode.ARCHIVED);
        }

        Optional<SnapshotResponse.FlagSnapshot> flag = findFlag(snapshot, flagKey);
        if (flag.isEmpty()) {
            if (failIfMissing && !flagExists(requestAppFromSnapshot(snapshot), flagKey)) {
                throw new FmsException(FmsErrorCode.FLAG_NOT_FOUND, "Flag not found: " + flagKey);
            }
            return new BatchFlagResult(flagKey, false, false, ReasonCode.NOT_PUBLISHED);
        }

        EvaluationResult result = ruleEngine.evaluate(flagKey, context, toFlagMap(flag.get()));
        return new BatchFlagResult(flagKey, result.value(), result.enabled(), result.reasonCode());
    }

    private static String requestAppFromSnapshot(SnapshotResponse snapshot) {
        return snapshot.appId();
    }

    private boolean flagExists(String appId, String flagKey) {
        return featureFlagRepository.findByApplication_SlugAndKey(appId, flagKey).isPresent();
    }

    private SnapshotResponse resolveSnapshot(String environment, String appId, Long pinnedVersion) {
        if (pinnedVersion != null) {
            Optional<SnapshotResponse> cached = snapshotCacheService.getSnapshot(environment, appId, pinnedVersion);
            if (cached.isPresent()) {
                return cached.get();
            }
            long currentVersion = snapshotLoaderService.resolveCurrentVersion(environment, appId);
            if (pinnedVersion == currentVersion && currentVersion > 0) {
                return snapshotCacheService.getSnapshot(environment, appId, currentVersion)
                        .orElseGet(() -> snapshotLoaderService.loadFullSnapshot(environment, appId));
            }
            throw new FmsException(
                    FmsErrorCode.VERSION_NOT_FOUND,
                    "Config version not found: " + pinnedVersion);
        }

        long currentVersion = snapshotLoaderService.resolveCurrentVersion(environment, appId);
        if (currentVersion <= 0) {
            return emptySnapshot(environment, appId);
        }

        return snapshotCacheService.getSnapshot(environment, appId, currentVersion)
                .orElseGet(() -> snapshotLoaderService.loadFullSnapshot(environment, appId));
    }

    private static SnapshotResponse emptySnapshot(String environment, String appId) {
        return new SnapshotResponse(environment, appId, 0L, null, "full", null, List.of(), List.of());
    }

    private static Optional<SnapshotResponse.FlagSnapshot> findFlag(SnapshotResponse snapshot, String flagKey) {
        return snapshot.flags().stream()
                .filter(flag -> flagKey.equals(flag.key()))
                .findFirst();
    }

    private static Map<String, Object> toFlagMap(SnapshotResponse.FlagSnapshot flag) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("key", flag.key());
        map.put("type", flag.type());
        map.put("defaultValue", flag.defaultValue());
        map.put("status", flag.status());
        map.put("rolloutSalt", flag.rolloutSalt());
        map.put("rules", flag.rules());
        if (flag.releaseId() != null) {
            map.put("releaseId", flag.releaseId());
        }
        return map;
    }

    private EvaluationContext toContext(EvaluateRequest.EvaluationContextDto dto) {
        return new EvaluationContext(
                dto.userId(),
                dto.deviceId(),
                dto.region(),
                dto.appVersion(),
                dto.customAttributes() == null ? Map.of() : dto.customAttributes());
    }
}
