package com.fms.explain.dto;

import java.util.List;
import java.util.Map;

public record ExplainResponse(
        String flagKey,
        boolean enabled,
        Object value,
        String type,
        long configVersion,
        ReleaseInfo release,
        Map<String, Object> context,
        Integer bucket,
        List<DecisionStep> decisionTrace,
        String matchedRuleId,
        String reasonCode,
        String evaluationMode,
        String schemaVersion
) {
    public record ReleaseInfo(String releaseId, String version) {
    }

    public record DecisionStep(
            String step,
            String ruleId,
            String ruleName,
            String result,
            String detail
    ) {
    }
}
