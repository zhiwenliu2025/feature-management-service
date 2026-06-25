package com.fms.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateFlagRequest(
        @NotBlank String appId,
        @NotBlank
        @Pattern(regexp = "^[a-z][a-z0-9_]{0,127}$")
        String key,
        @NotBlank @Size(max = 256) String name,
        String description,
        @NotBlank String type,
        @NotNull Object defaultValue,
        List<@NotBlank String> tags
) {
}
