package com.fms.ruleengine;

public record TraceStep(
        String step,
        String ruleId,
        String ruleName,
        String result,
        String detail
) {
}
