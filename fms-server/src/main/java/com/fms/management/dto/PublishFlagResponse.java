package com.fms.management.dto;

import java.time.Instant;

public record PublishFlagResponse(
        String flagKey,
        String appId,
        String environment,
        long configVersion,
        int flagVersion,
        String publishJobId,
        String status,
        Instant publishedAt,
        String publishedBy
) {
}
