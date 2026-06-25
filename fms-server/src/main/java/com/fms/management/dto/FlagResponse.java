package com.fms.management.dto;

import com.fms.domain.enums.FlagStatus;
import com.fms.domain.enums.FlagType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FlagResponse(
        UUID id,
        String appId,
        String key,
        String name,
        String description,
        String type,
        Object defaultValue,
        FlagStatus status,
        List<String> tags,
        Instant createdAt,
        String createdBy
) {
}
