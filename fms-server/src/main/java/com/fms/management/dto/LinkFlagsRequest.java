package com.fms.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record LinkFlagsRequest(
        @NotEmpty List<String> flagKeys,
        @NotBlank String appId,
        @NotBlank String environment
) {
}
