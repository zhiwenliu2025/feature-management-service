package com.fms.management.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ReleaseDetailResponse(
        UUID id,
        String releaseId,
        String version,
        String title,
        String description,
        Map<String, Object> metadata,
        List<LinkedFlagResponse> flags,
        Instant createdAt,
        String createdBy
) {
    public record LinkedFlagResponse(
            String appId,
            String flagKey,
            String environment,
            Long configVersion
    ) {
    }
}
