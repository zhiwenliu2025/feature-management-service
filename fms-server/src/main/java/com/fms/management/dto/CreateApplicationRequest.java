package com.fms.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateApplicationRequest(
        @NotBlank @Size(max = 64) String slug,
        @NotBlank @Size(max = 128) String name,
        String description,
        @Size(max = 128) String ownerTeam
) {
}
