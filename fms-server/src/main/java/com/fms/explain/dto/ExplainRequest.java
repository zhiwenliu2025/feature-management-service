package com.fms.explain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record ExplainRequest(
        @NotBlank String environment,
        @NotBlank String appId,
        @NotNull @Valid EvaluateContextDto context,
        boolean includeCustomAttributes
) {
    public record EvaluateContextDto(
            String userId,
            String deviceId,
            String region,
            String appVersion,
            Map<String, Object> customAttributes
    ) {
    }
}
