package com.fms.management.dto;

import java.time.Instant;
import java.util.UUID;

public record ReleaseResponse(
        UUID id,
        String releaseId,
        String version,
        String title,
        Instant createdAt,
        String createdBy
) {
}
