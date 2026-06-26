package com.fms.management.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateFlagRequest(
        @Size(max = 256) String name,
        String description,
        List<@Size(max = 64) String> tags
) {
}
