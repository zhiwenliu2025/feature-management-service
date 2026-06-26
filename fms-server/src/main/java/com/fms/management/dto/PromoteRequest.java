package com.fms.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record PromoteRequest(
        @NotBlank String sourceEnvironment,
        @NotEmpty List<String> flagKeys,
        @NotBlank String appId,
        String releaseId,
        String comment
) {
}
