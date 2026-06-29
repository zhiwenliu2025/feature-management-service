package com.fms.ruleengine;

import java.util.Map;

public interface RuleEngine {

    EvaluationResult evaluate(String flagKey, EvaluationContext context, Map<String, Object> flagSnapshot);
}
