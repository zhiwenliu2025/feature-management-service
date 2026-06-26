package com.fms.management.dto;

import java.util.UUID;

public record RuleResponse(
        UUID id,
        int priority,
        String name,
        Object conditions,
        Object value,
        boolean isEnabled
) {
}
