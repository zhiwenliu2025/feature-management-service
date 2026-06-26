package com.fms.management.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RuleInput(
        @Min(0) int priority,
        String name,
        @NotNull Object conditions,
        @NotNull Object value,
        boolean isEnabled
) {
}
