package com.fms.management.dto;

import com.fms.domain.enums.FlagStatus;

import java.time.Instant;

public record FlagSummaryResponse(
        String appId,
        String key,
        String name,
        String type,
        FlagStatus status,
        Object defaultValue,
        java.util.List<String> tags,
        boolean draftDirty,
        Instant updatedAt
) {
}
