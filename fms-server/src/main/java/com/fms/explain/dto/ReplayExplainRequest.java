package com.fms.explain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record ReplayExplainRequest(
        @NotBlank String environment,
        @NotBlank String appId,
        Long configVersion,
        Instant timestamp,
        @NotNull @Valid ExplainRequest.EvaluateContextDto context,
        boolean includeCustomAttributes
) {
}
