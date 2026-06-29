package com.fms.ruleengine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class ConditionMatcher {

    private ConditionMatcher() {
    }

    static boolean matches(Map<String, Object> conditions, EvaluationContext context, String flagKey, String rolloutSalt) {
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }

        for (Map.Entry<String, Object> entry : conditions.entrySet()) {
            String field = entry.getKey();
            Object condition = entry.getValue();

            if ("rolloutPercent".equals(field)) {
                Number percent = condition instanceof Number number
                        ? number
                        : condition instanceof Map<?, ?> map && map.get("value") instanceof Number value
                                ? value
                                : null;
                if (percent == null || !matchesRolloutPercent(percent, context, flagKey, rolloutSalt)) {
                    return false;
                }
                continue;
            }

            if (!(condition instanceof Map<?, ?> conditionMap)) {
                return false;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> typedCondition = (Map<String, Object>) conditionMap;

            if ("customAttributes".equals(field)) {
                if (!matchesCustomAttributes(typedCondition, context.customAttributes())) {
                    return false;
                }
                continue;
            }

            if ("segment".equals(field)) {
                return false;
            }

            String actual = resolveField(field, context);
            if (!matchesFieldCondition(actual, typedCondition)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesRolloutPercent(
            Number percent,
            EvaluationContext context,
            String flagKey,
            String rolloutSalt) {
        String bucketingKey = context.userId() != null && !context.userId().isBlank()
                ? context.userId()
                : context.deviceId() != null && !context.deviceId().isBlank()
                        ? context.deviceId()
                        : null;
        if (bucketingKey == null) {
            return false;
        }

        int bucket = MurmurHash3.bucket(flagKey, bucketingKey, rolloutSalt);
        int threshold = (int) Math.round(percent.doubleValue() * 100);
        return bucket < threshold;
    }

    private static boolean matchesCustomAttributes(Map<String, Object> conditions, Map<String, Object> attributes) {
        Map<String, Object> safeAttributes = attributes == null ? Map.of() : attributes;
        for (Map.Entry<String, Object> entry : conditions.entrySet()) {
            if (!(entry.getValue() instanceof Map<?, ?> conditionMap)) {
                return false;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> typedCondition = (Map<String, Object>) conditionMap;
            Object actual = safeAttributes.get(entry.getKey());
            if (!matchesScalar(actual, typedCondition)) {
                return false;
            }
        }
        return true;
    }

    private static String resolveField(String field, EvaluationContext context) {
        return switch (field) {
            case "userId" -> context.userId();
            case "deviceId" -> context.deviceId();
            case "region" -> context.region();
            case "appVersion" -> context.appVersion();
            default -> null;
        };
    }

    private static boolean matchesFieldCondition(String actual, Map<String, Object> condition) {
        if (condition.containsKey("rolloutPercent")) {
            return true;
        }
        return matchesScalar(actual, condition);
    }

    private static boolean matchesScalar(Object actual, Map<String, Object> condition) {
        String operator = String.valueOf(condition.getOrDefault("operator", "eq"));
        return switch (operator) {
            case "eq" -> Objects.equals(stringValue(actual), stringValue(condition.get("value")));
            case "neq" -> !Objects.equals(stringValue(actual), stringValue(condition.get("value")));
            case "in" -> containsValue(condition.get("values"), actual);
            case "not_in" -> !containsValue(condition.get("values"), actual);
            case "semver_gte" -> SemverComparator.compare(stringValue(actual), stringValue(condition.get("value"))) >= 0;
            case "semver_lte" -> SemverComparator.compare(stringValue(actual), stringValue(condition.get("value"))) <= 0;
            case "in_segment" -> false;
            default -> false;
        };
    }

    private static boolean containsValue(Object values, Object actual) {
        if (!(values instanceof Collection<?> collection)) {
            return false;
        }
        String actualValue = stringValue(actual);
        return collection.stream().map(ConditionMatcher::stringValue).anyMatch(actualValue::equals);
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    static String describeMatch(
            Map<String, Object> conditions,
            EvaluationContext context,
            String flagKey,
            String rolloutSalt) {
        if (conditions == null || conditions.isEmpty()) {
            return "no conditions";
        }

        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, Object> entry : conditions.entrySet()) {
            String field = entry.getKey();
            Object condition = entry.getValue();

            if ("rolloutPercent".equals(field)) {
                Number percent = condition instanceof Number number
                        ? number
                        : condition instanceof Map<?, ?> map && map.get("value") instanceof Number value
                                ? value
                                : null;
                if (percent != null) {
                    String bucketingKey = context.userId() != null && !context.userId().isBlank()
                            ? context.userId()
                            : context.deviceId();
                    int bucket = MurmurHash3.bucket(flagKey, bucketingKey, rolloutSalt);
                    int threshold = (int) Math.round(percent.doubleValue() * 100);
                    parts.add("bucket " + bucket + " < " + threshold + " (" + percent + "%)");
                }
                continue;
            }

            if (!(condition instanceof Map<?, ?> conditionMap)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> typedCondition = (Map<String, Object>) conditionMap;

            if ("customAttributes".equals(field)) {
                typedCondition.forEach((key, value) -> {
                    if (value instanceof Map<?, ?> attrCondition) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> typed = (Map<String, Object>) attrCondition;
                        Object actual = context.customAttributes() == null ? null : context.customAttributes().get(key);
                        parts.add(key + " " + stringValue(actual) + " "
                                + typed.getOrDefault("operator", "eq") + " " + typed.get("value"));
                    }
                });
                continue;
            }

            if ("segment".equals(field)) {
                parts.add("segment not supported");
                continue;
            }

            String actual = resolveField(field, context);
            String operator = String.valueOf(typedCondition.getOrDefault("operator", "eq"));
            if ("in".equals(operator) || "not_in".equals(operator)) {
                parts.add(field + " " + stringValue(actual) + " " + operator + " " + typedCondition.get("values"));
            } else {
                parts.add(field + " " + stringValue(actual) + " " + operator + " " + typedCondition.get("value"));
            }
        }
        return String.join("; ", parts);
    }

    static Integer resolveBucket(Map<String, Object> conditions, EvaluationContext context, String flagKey, String rolloutSalt) {
        if (conditions == null) {
            return null;
        }
        Object rollout = conditions.get("rolloutPercent");
        if (rollout == null) {
            return null;
        }
        String bucketingKey = context.userId() != null && !context.userId().isBlank()
                ? context.userId()
                : context.deviceId() != null && !context.deviceId().isBlank()
                        ? context.deviceId()
                        : null;
        if (bucketingKey == null) {
            return null;
        }
        return MurmurHash3.bucket(flagKey, bucketingKey, rolloutSalt);
    }
}
