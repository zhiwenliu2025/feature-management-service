package com.fms.management.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ApiKeyResponse(
        UUID id,
        String keyPrefix,
        String name,
        List<String> scopes,
        Instant expiresAt,
        Instant revokedAt,
        Instant createdAt
) {
}
