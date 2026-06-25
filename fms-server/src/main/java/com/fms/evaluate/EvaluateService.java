package com.fms.evaluate;

import com.fms.cache.SnapshotCacheService;
import com.fms.evaluate.dto.BatchEvaluateRequest;
import com.fms.evaluate.dto.BatchEvaluateResponse;
import com.fms.evaluate.dto.EvaluateRequest;
import com.fms.evaluate.dto.EvaluateResponse;
import com.fms.ruleengine.EvaluationContext;
import com.fms.ruleengine.EvaluationResult;
import com.fms.ruleengine.RuleEngine;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class EvaluateService {

    private final RuleEngine ruleEngine;
    private final SnapshotCacheService snapshotCacheService;

    public EvaluateService(RuleEngine ruleEngine, SnapshotCacheService snapshotCacheService) {
        this.ruleEngine = ruleEngine;
        this.snapshotCacheService = snapshotCacheService;
    }

    public EvaluateResponse evaluate(String flagKey, EvaluateRequest request) {
        long start = System.nanoTime();
        long configVersion = request.configVersion() != null
                ? request.configVersion()
                : snapshotCacheService.getCurrentVersion(request.environment(), request.appId());

        EvaluationContext context = toContext(request.context());
        EvaluationResult result = ruleEngine.evaluate(flagKey, context, null);

        long latencyMs = (System.nanoTime() - start) / 1_000_000;
        return new EvaluateResponse(
                flagKey,
                result.value(),
                result.enabled(),
                "boolean",
                configVersion,
                "remote",
                result.reasonCode(),
                latencyMs);
    }

    public BatchEvaluateResponse evaluateBatch(BatchEvaluateRequest request) {
        long start = System.nanoTime();
        long configVersion = snapshotCacheService.getCurrentVersion(request.environment(), request.appId());

        List<EvaluateResponse> results = request.flagKeys().stream()
                .map(flagKey -> evaluate(flagKey, new EvaluateRequest(
                        request.environment(),
                        request.appId(),
                        configVersion,
                        request.context())))
                .toList();

        long latencyMs = (System.nanoTime() - start) / 1_000_000;
        return new BatchEvaluateResponse(configVersion, "remote", results, latencyMs);
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
