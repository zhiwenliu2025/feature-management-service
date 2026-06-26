package com.fms.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RollbackFlagRequest(
        @NotBlank String appId,
        @NotBlank String environment,
        @NotNull int targetFlagVersion,
        String comment
) {
}
