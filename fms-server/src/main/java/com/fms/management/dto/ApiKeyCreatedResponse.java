package com.fms.management.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ApiKeyCreatedResponse(
        UUID id,
        String keyPrefix,
        String apiKey,
        List<String> scopes,
        Instant expiresAt,
        Instant createdAt
) {
}
