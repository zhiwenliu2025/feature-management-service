package com.fms.console.client.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class EnvironmentDtos {

  private EnvironmentDtos() {}

  public record EnvironmentDto(
      String name,
      String displayName,
      short sortOrder,
      boolean production
  ) {}

  public record EnvironmentConfigDto(
      String environment,
      long currentConfigVersion,
      Instant updatedAt
  ) {}

  public record PromoteResultDto(
      String targetEnvironment,
      String sourceEnvironment,
      List<String> promotedFlags,
      long configVersion,
      List<String> publishJobIds
  ) {}

  public record PromoteDto(
      String sourceEnvironment,
      List<String> flagKeys,
      String appId,
      String releaseId,
      String comment
  ) {}
}
