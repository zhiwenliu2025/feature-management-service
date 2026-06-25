package com.fms.ruleengine;

/**
 * Stub rule engine — returns default value until full rule matching is implemented.
 */
public class DefaultRuleEngine implements RuleEngine {

    @Override
    public EvaluationResult evaluate(String flagKey, EvaluationContext context, String snapshotJson) {
        return EvaluationResult.defaultValue(false, "DEFAULT_VALUE", null);
    }
}
