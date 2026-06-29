package com.fms.ruleengine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class DefaultRuleEngine implements RuleEngine {

    @Override
    public EvaluationResult evaluate(String flagKey, EvaluationContext context, Map<String, Object> flagSnapshot) {
        ExplainResult explained = explain(flagKey, context, flagSnapshot);
        return new EvaluationResult(
                explained.value(),
                explained.enabled(),
                explained.reasonCode(),
                null);
    }

    @Override
    public ExplainResult explain(String flagKey, EvaluationContext context, Map<String, Object> flagSnapshot) {
        List<TraceStep> trace = new ArrayList<>();

        if (flagSnapshot == null || flagSnapshot.isEmpty()) {
            trace.add(new TraceStep(
                    "environment_check",
                    null,
                    null,
                    "fail",
                    "flag not published in environment"));
            return new ExplainResult(false, false, ReasonCode.NOT_PUBLISHED, null, null, trace);
        }

        String status = stringValue(flagSnapshot.get("status"));
        Object defaultValue = flagSnapshot.get("defaultValue");
        String type = stringValue(flagSnapshot.get("type"));
        String rolloutSalt = stringValue(flagSnapshot.get("rolloutSalt"));

        if ("archived".equalsIgnoreCase(status)) {
            trace.add(new TraceStep(
                    "environment_check",
                    null,
                    null,
                    "fail",
                    "flag archived"));
            return explainResult(defaultValue, type, ReasonCode.ARCHIVED, null, null, trace);
        }
        if (!"published".equalsIgnoreCase(status)) {
            trace.add(new TraceStep(
                    "environment_check",
                    null,
                    null,
                    "fail",
                    "flag not published in environment"));
            return explainResult(defaultValue, type, ReasonCode.NOT_PUBLISHED, null, null, trace);
        }

        trace.add(new TraceStep(
                "environment_check",
                null,
                null,
                "pass",
                "published in environment"));

        List<Map<String, Object>> rules = extractRules(flagSnapshot.get("rules"));
        rules.sort(Comparator.comparingInt(rule -> intValue(rule.get("priority"))));

        Integer bucket = null;
        for (Map<String, Object> rule : rules) {
            @SuppressWarnings("unchecked")
            Map<String, Object> conditions = rule.get("conditions") instanceof Map<?, ?> map
                    ? (Map<String, Object>) map
                    : Map.of();
            if (ConditionMatcher.matches(conditions, context, flagKey, rolloutSalt)) {
                Integer ruleBucket = ConditionMatcher.resolveBucket(conditions, context, flagKey, rolloutSalt);
                bucket = ruleBucket;
                String ruleId = stringValue(rule.get("id"));
                String ruleName = stringValue(rule.get("name"));
                trace.add(new TraceStep(
                        "rule_evaluation",
                        ruleId,
                        ruleName,
                        "match",
                        ConditionMatcher.describeMatch(conditions, context, flagKey, rolloutSalt)));
                return explainResult(
                        rule.get("value"),
                        type,
                        ReasonCode.RULE_MATCH,
                        bucket,
                        ruleId,
                        trace);
            }
        }

        String reasonCode = rules.isEmpty() ? ReasonCode.DEFAULT_VALUE : ReasonCode.NO_MATCH;
        trace.add(new TraceStep(
                "rule_evaluation",
                null,
                null,
                "no_match",
                rules.isEmpty() ? "no rules configured" : "no rule matched context"));
        return explainResult(defaultValue, type, reasonCode, bucket, null, trace);
    }

    private static ExplainResult explainResult(
            Object value,
            String type,
            String reasonCode,
            Integer bucket,
            String matchedRuleId,
            List<TraceStep> trace) {
        return new ExplainResult(
                value,
                computeEnabled(type, value, reasonCode),
                reasonCode,
                bucket,
                matchedRuleId,
                trace);
    }

    public static boolean computeEnabled(String type, Object value, String reasonCode) {
        if (ReasonCode.RULE_MATCH.equals(reasonCode) || ReasonCode.KILL_SWITCH.equals(reasonCode)) {
            if ("boolean".equals(type)) {
                return value instanceof Boolean bool && bool;
            }
            return true;
        }
        if ("boolean".equals(type)) {
            return value instanceof Boolean bool && bool;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> extractRules(Object rawRules) {
        if (!(rawRules instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> rules = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                rules.add((Map<String, Object>) map);
            }
        }
        return rules;
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return 0;
        }
        return Integer.parseInt(String.valueOf(value));
    }
}
