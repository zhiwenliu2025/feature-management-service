package com.fms.console.client.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class ExplainDtos {

  private ExplainDtos() {}

  public record ExplainRequestDto(
      String environment,
      String appId,
      EvaluateContextDto context,
      boolean includeCustomAttributes
  ) {}

  public record EvaluateContextDto(
      String userId,
      String deviceId,
      String region,
      String appVersion,
      Map<String, Object> customAttributes
  ) {}

  public record ExplainResponseDto(
      String flagKey,
      boolean enabled,
      Object value,
      String type,
      long configVersion,
      ReleaseInfoDto release,
      Map<String, Object> context,
      Integer bucket,
      List<DecisionStepDto> decisionTrace,
      String matchedRuleId,
      String reasonCode,
      String evaluationMode,
      String schemaVersion
  ) {}

  public record ReleaseInfoDto(String releaseId, String version) {}

  public record DecisionStepDto(
      String step,
      String ruleId,
      String ruleName,
      String result,
      String detail
  ) {}

  public record ReplayExplainRequestDto(
      String environment,
      String appId,
      Long configVersion,
      Instant timestamp,
      EvaluateContextDto context,
      boolean includeCustomAttributes
  ) {}
}
