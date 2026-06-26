package com.fms.management.dto;

import java.time.Instant;

public record EnvironmentConfigResponse(
        String environment,
        long currentConfigVersion,
        Instant updatedAt
) {
}
