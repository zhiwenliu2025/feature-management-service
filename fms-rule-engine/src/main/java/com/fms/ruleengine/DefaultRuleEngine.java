package com.fms.ruleengine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class DefaultRuleEngine implements RuleEngine {

    @Override
    public EvaluationResult evaluate(String flagKey, EvaluationContext context, Map<String, Object> flagSnapshot) {
        if (flagSnapshot == null || flagSnapshot.isEmpty()) {
            return EvaluationResult.defaultValue(false, ReasonCode.NOT_PUBLISHED, null);
        }

        String status = stringValue(flagSnapshot.get("status"));
        Object defaultValue = flagSnapshot.get("defaultValue");
        String type = stringValue(flagSnapshot.get("type"));
        String rolloutSalt = stringValue(flagSnapshot.get("rolloutSalt"));

        if ("archived".equalsIgnoreCase(status)) {
            return result(defaultValue, type, ReasonCode.ARCHIVED);
        }
        if (!"published".equalsIgnoreCase(status)) {
            return result(defaultValue, type, ReasonCode.NOT_PUBLISHED);
        }

        List<Map<String, Object>> rules = extractRules(flagSnapshot.get("rules"));
        rules.sort(Comparator.comparingInt(rule -> intValue(rule.get("priority"))));

        for (Map<String, Object> rule : rules) {
            @SuppressWarnings("unchecked")
            Map<String, Object> conditions = rule.get("conditions") instanceof Map<?, ?> map
                    ? (Map<String, Object>) map
                    : Map.of();
            if (ConditionMatcher.matches(conditions, context, flagKey, rolloutSalt)) {
                return result(rule.get("value"), type, ReasonCode.RULE_MATCH);
            }
        }

        String reasonCode = rules.isEmpty() ? ReasonCode.DEFAULT_VALUE : ReasonCode.NO_MATCH;
        return result(defaultValue, type, reasonCode);
    }

    private static EvaluationResult result(Object value, String type, String reasonCode) {
        return new EvaluationResult(value, computeEnabled(type, value, reasonCode), reasonCode, null);
    }

    static boolean computeEnabled(String type, Object value, String reasonCode) {
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
