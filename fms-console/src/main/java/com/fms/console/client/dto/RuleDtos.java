package com.fms.console.client.dto;

import java.util.List;

public final class RuleDtos {

  private RuleDtos() {}

  public record RuleInputDto(
      int priority,
      String name,
      Object conditions,
      Object value,
      boolean isEnabled
  ) {}

  public record ReplaceRulesDto(
      String environment,
      List<RuleInputDto> rules
  ) {}

  public record UpdateRuleDto(
      Integer priority,
      String name,
      Object conditions,
      Object value,
      Boolean isEnabled
  ) {}
}
