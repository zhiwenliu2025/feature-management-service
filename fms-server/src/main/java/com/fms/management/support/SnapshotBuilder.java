package com.fms.management.support;

import com.fms.domain.FeatureFlagEntity;
import com.fms.domain.FlagRuleEntity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SnapshotBuilder {

    private SnapshotBuilder() {
    }

    public static Map<String, Object> build(FeatureFlagEntity flag, List<FlagRuleEntity> rules, String releaseId) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("key", flag.getKey());
        snapshot.put("type", flag.getType().externalName());
        snapshot.put("defaultValue", flag.getDefaultValue());
        snapshot.put("status", flag.getStatus().name());
        snapshot.put("rolloutSalt", flag.getRolloutSalt());
        snapshot.put("rules", rules.stream().filter(FlagRuleEntity::isEnabled).map(SnapshotBuilder::toRuleMap).toList());
        if (releaseId != null) {
            snapshot.put("releaseId", releaseId);
        }
        return snapshot;
    }

    private static Map<String, Object> toRuleMap(FlagRuleEntity rule) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", rule.getId().toString());
        map.put("priority", rule.getPriority());
        map.put("conditions", rule.getConditions());
        map.put("value", rule.getValue());
        return map;
    }
}
