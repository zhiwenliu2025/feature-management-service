package com.fms.management.dto;

import java.time.Instant;
import java.util.List;

public record KillSwitchResponse(
        String flagKey,
        String environment,
        String scope,
        String regionCode,
        boolean isActive,
        Object forcedValue,
        Instant activatedAt,
        String activatedBy,
        Long configVersion
) {
    public record KillSwitchListResponse(List<KillSwitchResponse> overrides) {
    }
}
