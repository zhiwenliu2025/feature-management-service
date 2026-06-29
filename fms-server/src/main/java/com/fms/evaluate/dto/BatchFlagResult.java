package com.fms.evaluate.dto;

public record BatchFlagResult(
        String flagKey,
        Object value,
        boolean enabled,
        String reasonCode
) {
}
