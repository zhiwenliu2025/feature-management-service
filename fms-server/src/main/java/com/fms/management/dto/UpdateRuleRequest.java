package com.fms.management.dto;

import jakarta.validation.constraints.Min;

public record UpdateRuleRequest(
        @Min(0) Integer priority,
        String name,
        Object conditions,
        Object value,
        Boolean isEnabled
) {
}
