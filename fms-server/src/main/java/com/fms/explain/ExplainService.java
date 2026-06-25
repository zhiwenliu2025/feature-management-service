package com.fms.explain;

import com.fms.cache.SnapshotCacheService;
import com.fms.explain.dto.ExplainRequest;
import com.fms.explain.dto.ExplainResponse;
import com.fms.explain.dto.ReplayExplainRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ExplainService {

    private final SnapshotCacheService snapshotCacheService;

    public ExplainService(SnapshotCacheService snapshotCacheService) {
        this.snapshotCacheService = snapshotCacheService;
    }

    public ExplainResponse explain(String flagKey, ExplainRequest request) {
        long configVersion = snapshotCacheService.getCurrentVersion(request.environment(), request.appId());
        return buildStubResponse(flagKey, configVersion, request.context(), "live");
    }

    public ExplainResponse replay(String flagKey, ReplayExplainRequest request) {
        long configVersion = request.configVersion() != null
                ? request.configVersion()
                : snapshotCacheService.getCurrentVersion(request.environment(), request.appId());
        return buildStubResponse(flagKey, configVersion, request.context(), "replay");
    }

    private ExplainResponse buildStubResponse(
            String flagKey,
            long configVersion,
            ExplainRequest.EvaluateContextDto context,
            String mode) {
        String maskedUser = context.userId() == null ? null : maskUserId(context.userId());
        return new ExplainResponse(
                flagKey,
                false,
                false,
                "boolean",
                configVersion,
                null,
                Map.of("userId", maskedUser == null ? "" : maskedUser, "region", context.region() == null ? "" : context.region()),
                null,
                List.of(new ExplainResponse.DecisionStep(
                        "environment_check",
                        null,
                        null,
                        "pass",
                        "published in environment")),
                null,
                "DEFAULT_VALUE",
                mode,
                "1.0");
    }

    private String maskUserId(String userId) {
        if (userId.length() <= 4) {
            return "usr_***";
        }
        return userId.substring(0, 4) + "***";
    }
}
