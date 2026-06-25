package com.fms.ruleengine;

public record EvaluationResult(
        Object value,
        boolean enabled,
        String reasonCode,
        Long configVersion
) {
    public static EvaluationResult defaultValue(Object defaultValue, String reasonCode, Long configVersion) {
        boolean enabled = defaultValue instanceof Boolean b && b;
        return new EvaluationResult(defaultValue, enabled, reasonCode, configVersion);
    }
}
