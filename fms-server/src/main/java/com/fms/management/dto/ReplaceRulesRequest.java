package com.fms.management.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReplaceRulesRequest(
        @NotBlank String environment,
        @NotNull List<@Valid RuleInput> rules
) {
}
