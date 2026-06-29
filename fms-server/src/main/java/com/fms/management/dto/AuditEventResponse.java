package com.fms.management.dto;

import com.fms.domain.enums.AuditAction;

import java.time.Instant;
import java.util.Map;

public record AuditEventResponse(
        String id,
        String actor,
        AuditAction action,
        String resourceType,
        String resourceId,
        String environment,
        Map<String, Object> diff,
        Map<String, Object> metadata,
        Instant createdAt
) {
}
