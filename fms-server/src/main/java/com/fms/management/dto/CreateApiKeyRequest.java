package com.fms.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public record CreateApiKeyRequest(
        @NotBlank @Size(max = 128) String name,
        List<@NotBlank String> scopes,
        Instant expiresAt
) {
}
