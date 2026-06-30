package com.fms.console.client.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class FlagDtos {

  private FlagDtos() {}

  public record FlagSummaryDto(
      String appId,
      String key,
      String name,
      String type,
      String status,
      Object defaultValue,
      List<String> tags,
      boolean draftDirty,
      Instant updatedAt
  ) {}

  public record EnvironmentStateDto(
      String environment,
      boolean isPublished,
      Long latestConfigVersion,
      boolean draftDirty
  ) {}

  public record RuleDto(
      UUID id,
      int priority,
      String name,
      Object conditions,
      Object value,
      boolean isEnabled
  ) {}

  public record FlagDetailDto(
      UUID id,
      String appId,
      String key,
      String name,
      String description,
      String type,
      Object defaultValue,
      String status,
      List<String> tags,
      List<EnvironmentStateDto> environmentStates,
      Map<String, List<RuleDto>> rules,
      Instant createdAt,
      Instant updatedAt,
      String createdBy
  ) {}

  public record CreateFlagDto(
      String appId,
      String key,
      String name,
      String description,
      String type,
      Object defaultValue,
      List<String> tags
  ) {}

  public record UpdateFlagDto(
      String name,
      String description,
      Object defaultValue,
      List<String> tags
  ) {}

  public record PublishFlagDto(
      String environment,
      String releaseId,
      String comment
  ) {}

  public record PublishFlagResultDto(
      String flagKey,
      String appId,
      String environment,
      long configVersion,
      int flagVersion,
      String publishJobId,
      String status,
      Instant publishedAt,
      String publishedBy
  ) {}

  public record RollbackFlagDto(
      String appId,
      String environment,
      int targetVersion,
      String comment
  ) {}

  public record FlagVersionSummaryDto(
      int flagVersion,
      long configVersion,
      String environment,
      String publishedBy,
      Instant publishedAt,
      String comment
  ) {}

  public record FlagVersionDetailDto(
      int flagVersion,
      long configVersion,
      String environment,
      Object snapshot,
      String publishedBy,
      Instant publishedAt,
      String comment
  ) {}
}
