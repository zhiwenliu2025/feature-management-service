package com.fms.evaluate.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record EvaluateRequest(
        @NotBlank String environment,
        @NotBlank String appId,
        Long configVersion,
        @NotNull @Valid EvaluationContextDto context
) {
    public record EvaluationContextDto(
            String userId,
            String deviceId,
            String region,
            String appVersion,
            Map<String, Object> customAttributes
    ) {
    }
}
