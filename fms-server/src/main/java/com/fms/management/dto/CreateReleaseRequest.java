package com.fms.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record CreateReleaseRequest(
        @NotBlank @Size(max = 128) String releaseId,
        @NotBlank @Size(max = 64) String version,
        @Size(max = 256) String title,
        String description,
        Map<String, Object> metadata
) {
}
