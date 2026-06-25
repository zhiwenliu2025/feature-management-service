package com.fms.evaluate.dto;

public record EvaluateResponse(
        String flagKey,
        Object value,
        boolean enabled,
        String type,
        long configVersion,
        String evaluationMode,
        String reasonCode,
        long latencyMs
) {
}
