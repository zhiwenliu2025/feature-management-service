package com.fms.management.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public record PublishFlagRequest(
        @NotBlank String environment,
        String releaseId,
        String comment,
        boolean killSwitch
) {
}
