package com.fms.management.dto;

import java.time.Instant;

public record FlagVersionDetailResponse(
        int flagVersion,
        long configVersion,
        String environment,
        Object snapshot,
        String publishedBy,
        Instant publishedAt,
        String comment
) {
}
