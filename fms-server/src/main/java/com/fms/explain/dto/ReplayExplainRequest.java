package com.fms.explain.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ReplayExplainRequest(
        String environment,
        String appId,
        Long configVersion,
        Instant timestamp,
        ExplainRequest.EvaluateContextDto context,
        boolean includeCustomAttributes
) {
}
