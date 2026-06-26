package com.fms.management.dto;

import java.util.List;

public record PromoteResponse(
        String targetEnvironment,
        String sourceEnvironment,
        List<String> promotedFlags,
        long configVersion,
        List<String> publishJobIds
) {
}
