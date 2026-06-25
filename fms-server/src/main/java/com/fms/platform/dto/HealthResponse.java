package com.fms.platform.dto;

import java.time.Instant;

public record HealthResponse(String status, Instant timestamp) {
}
