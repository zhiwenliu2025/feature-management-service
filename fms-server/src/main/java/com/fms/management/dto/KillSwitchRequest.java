package com.fms.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record KillSwitchRequest(
        @NotBlank String appId,
        @NotBlank String environment,
        @NotBlank String scope,
        String regionCode,
        @NotNull Object forcedValue,
        String comment
) {
}
