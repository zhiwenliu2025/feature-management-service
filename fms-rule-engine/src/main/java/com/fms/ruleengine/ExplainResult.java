package com.fms.ruleengine;

import java.util.List;

public record ExplainResult(
        Object value,
        boolean enabled,
        String reasonCode,
        Integer bucket,
        String matchedRuleId,
        List<TraceStep> decisionTrace
) {
}
