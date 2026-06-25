package com.fms.evaluate.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BatchEvaluateRequest(
        @NotBlank String environment,
        @NotBlank String appId,
        @NotEmpty @Size(max = 50) List<@NotBlank String> flagKeys,
        @NotNull @Valid EvaluateRequest.EvaluationContextDto context
) {
}
