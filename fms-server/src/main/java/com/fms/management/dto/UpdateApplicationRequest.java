package com.fms.management.dto;

import jakarta.validation.constraints.Size;

public record UpdateApplicationRequest(
        @Size(max = 128) String name,
        String description,
        @Size(max = 128) String ownerTeam
) {
}
