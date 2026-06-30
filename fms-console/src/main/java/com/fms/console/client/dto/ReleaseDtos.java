package com.fms.console.client.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ReleaseDtos {

  private ReleaseDtos() {}

  public record ReleaseSummaryDto(
      UUID id,
      String releaseId,
      String version,
      String title,
      Instant createdAt,
      String createdBy
  ) {}

  public record ReleaseDetailDto(
      UUID id,
      String releaseId,
      String version,
      String title,
      String description,
      Map<String, Object> metadata,
      List<LinkedFlagDto> flags,
      Instant createdAt,
      String createdBy
  ) {}

  public record LinkedFlagDto(
      String appId,
      String flagKey,
      String environment,
      Long configVersion
  ) {}

  public record CreateReleaseDto(
      String releaseId,
      String version,
      String title,
      String description,
      Map<String, Object> metadata
  ) {}

  public record LinkFlagsDto(
      String appId,
      List<String> flagKeys,
      String environment
  ) {}
}
