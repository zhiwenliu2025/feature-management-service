package com.fms.ruleengine;

public interface RuleEngine {

    EvaluationResult evaluate(String flagKey, EvaluationContext context, String snapshotJson);
}
