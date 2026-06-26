package com.fms.management.dto;

import com.fms.domain.enums.FlagStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record FlagDetailResponse(
        UUID id,
        String appId,
        String key,
        String name,
        String description,
        String type,
        Object defaultValue,
        FlagStatus status,
        List<String> tags,
        List<EnvironmentStateResponse> environmentStates,
        Map<String, List<RuleResponse>> rules,
        Instant createdAt,
        Instant updatedAt,
        String createdBy
) {
}
