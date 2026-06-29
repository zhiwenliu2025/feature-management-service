package com.fms.ruleengine;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultRuleEngineTest {

    private final DefaultRuleEngine engine = new DefaultRuleEngine();

    @Test
    void returnsNotPublishedWhenSnapshotMissing() {
        EvaluationResult result = engine.evaluate("flag_a", sampleContext("US"), null);

        assertEquals(ReasonCode.NOT_PUBLISHED, result.reasonCode());
        assertFalse(result.enabled());
    }

    @Test
    void returnsArchivedForArchivedFlag() {
        EvaluationResult result = engine.evaluate(
                "flag_a",
                sampleContext("US"),
                publishedFlag(false, "archived", List.of()));

        assertEquals(ReasonCode.ARCHIVED, result.reasonCode());
        assertFalse(result.enabled());
    }

    @Test
    void returnsDefaultValueWhenNoRulesMatch() {
        Map<String, Object> conditions = Map.of(
                "region", Map.of("operator", "in", "values", List.of("EU")));

        EvaluationResult result = engine.evaluate(
                "flag_a",
                sampleContext("US"),
                publishedFlag(false, "published", List.of(rule(10, conditions, true))));

        assertEquals(ReasonCode.NO_MATCH, result.reasonCode());
        assertFalse(result.enabled());
        assertEquals(false, result.value());
    }

    @Test
    void returnsRuleMatchWhenRegionMatches() {
        Map<String, Object> conditions = Map.of(
                "region", Map.of("operator", "in", "values", List.of("US", "CA")));

        EvaluationResult result = engine.evaluate(
                "flag_a",
                sampleContext("US"),
                publishedFlag(false, "published", List.of(rule(10, conditions, true))));

        assertEquals(ReasonCode.RULE_MATCH, result.reasonCode());
        assertTrue(result.enabled());
        assertEquals(true, result.value());
    }

    @Test
    void evaluatesRulesByAscendingPriority() {
        Map<String, Object> lowPriority = Map.of(
                "region", Map.of("operator", "in", "values", List.of("US")));
        Map<String, Object> highPriority = Map.of(
                "region", Map.of("operator", "in", "values", List.of("US")));

        EvaluationResult result = engine.evaluate(
                "flag_a",
                sampleContext("US"),
                publishedFlag(false, "published", List.of(
                        rule(20, lowPriority, "low"),
                        rule(10, highPriority, "high"))));

        assertEquals(ReasonCode.RULE_MATCH, result.reasonCode());
        assertEquals("high", result.value());
    }

    @Test
    void matchesSemverGteCondition() {
        Map<String, Object> conditions = Map.of(
                "appVersion", Map.of("operator", "semver_gte", "value", "3.2.0"));

        EvaluationResult result = engine.evaluate(
                "flag_a",
                contextWithVersion("3.2.1"),
                publishedFlag(false, "published", List.of(rule(10, conditions, true))));

        assertEquals(ReasonCode.RULE_MATCH, result.reasonCode());
        assertTrue(result.enabled());
    }

    @Test
    void matchesCustomAttributeEquality() {
        Map<String, Object> conditions = Map.of(
                "customAttributes", Map.of(
                        "loyaltyTier", Map.of("operator", "eq", "value", "gold")));

        EvaluationContext context = new EvaluationContext(
                "usr_1", null, "US", null, Map.of("loyaltyTier", "gold"));

        EvaluationResult result = engine.evaluate(
                "flag_a",
                context,
                publishedFlag(false, "published", List.of(rule(10, conditions, true))));

        assertEquals(ReasonCode.RULE_MATCH, result.reasonCode());
    }

    @Test
    void rolloutPercentRequiresBucketingKey() {
        Map<String, Object> conditions = Map.of("rolloutPercent", 100);

        EvaluationResult withoutUser = engine.evaluate(
                "flag_a",
                new EvaluationContext(null, null, "US", null, Map.of()),
                publishedFlag(false, "published", List.of(rule(10, conditions, true))));

        assertEquals(ReasonCode.NO_MATCH, withoutUser.reasonCode());

        EvaluationResult withUser = engine.evaluate(
                "flag_a",
                new EvaluationContext("usr_123", null, "US", null, Map.of()),
                publishedFlag(false, "published", List.of(rule(10, conditions, true))));

        assertEquals(ReasonCode.RULE_MATCH, withUser.reasonCode());
    }

    @Test
    void computeEnabledTreatsNonBooleanRuleMatchAsEnabled() {
        assertTrue(DefaultRuleEngine.computeEnabled("string", "variant_a", ReasonCode.RULE_MATCH));
        assertFalse(DefaultRuleEngine.computeEnabled("string", "variant_a", ReasonCode.DEFAULT_VALUE));
    }

    @Test
    void explainIncludesEnvironmentAndRuleTrace() {
        Map<String, Object> conditions = Map.of(
                "region", Map.of("operator", "in", "values", List.of("US")));

        ExplainResult result = engine.explain(
                "flag_a",
                sampleContext("US"),
                publishedFlag(false, "published", List.of(rule(10, conditions, true))));

        assertEquals(ReasonCode.RULE_MATCH, result.reasonCode());
        assertEquals(2, result.decisionTrace().size());
        assertEquals("environment_check", result.decisionTrace().get(0).step());
        assertEquals("rule_evaluation", result.decisionTrace().get(1).step());
        assertEquals("match", result.decisionTrace().get(1).result());
    }

    private static EvaluationContext sampleContext(String region) {
        return new EvaluationContext("usr_1", null, region, "3.2.1", Map.of());
    }

    private static EvaluationContext contextWithVersion(String appVersion) {
        return new EvaluationContext("usr_1", null, "US", appVersion, Map.of());
    }

    private static Map<String, Object> publishedFlag(
            Object defaultValue, String status, List<Map<String, Object>> rules) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("key", "flag_a");
        snapshot.put("type", "boolean");
        snapshot.put("defaultValue", defaultValue);
        snapshot.put("status", status);
        snapshot.put("rolloutSalt", "salt-123");
        snapshot.put("rules", rules);
        return snapshot;
    }

    private static Map<String, Object> rule(int priority, Map<String, Object> conditions, Object value) {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("priority", priority);
        rule.put("conditions", conditions);
        rule.put("value", value);
        return rule;
    }
}
