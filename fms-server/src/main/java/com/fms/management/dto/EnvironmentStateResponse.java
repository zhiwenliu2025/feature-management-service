package com.fms.management.dto;

public record EnvironmentStateResponse(
        String environment,
        boolean isPublished,
        Long latestConfigVersion,
        boolean draftDirty
) {
}
