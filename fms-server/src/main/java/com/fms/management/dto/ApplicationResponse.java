package com.fms.management.dto;

import com.fms.domain.enums.ApplicationStatus;

import java.time.Instant;
import java.util.UUID;

public record ApplicationResponse(
        UUID id,
        String slug,
        String name,
        String description,
        ApplicationStatus status,
        String ownerTeam,
        Instant createdAt,
        String createdBy
) {
}
