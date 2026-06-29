package com.fms.management.dto;

import jakarta.validation.constraints.NotBlank;

public record PublishFlagRequest(
        @NotBlank String environment,
        String releaseId,
        String comment,
        boolean killSwitch
) {
}
