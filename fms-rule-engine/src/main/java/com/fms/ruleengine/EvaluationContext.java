package com.fms.ruleengine;

import java.util.Map;

public record EvaluationContext(
        String userId,
        String deviceId,
        String region,
        String appVersion,
        Map<String, Object> customAttributes
) {
}
