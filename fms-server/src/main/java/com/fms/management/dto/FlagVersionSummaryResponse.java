package com.fms.management.dto;

import java.time.Instant;

public record FlagVersionSummaryResponse(
        int flagVersion,
        long configVersion,
        String environment,
        String publishedBy,
        Instant publishedAt,
        String comment
) {
}
