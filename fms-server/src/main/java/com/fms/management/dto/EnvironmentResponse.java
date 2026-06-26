package com.fms.management.dto;

public record EnvironmentResponse(
        String name,
        String displayName,
        short sortOrder,
        boolean production
) {
}
