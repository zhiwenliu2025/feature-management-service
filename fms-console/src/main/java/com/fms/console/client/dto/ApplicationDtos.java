package com.fms.console.client.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ApplicationDtos {

  private ApplicationDtos() {}

  public record ApplicationDto(
      UUID id,
      String slug,
      String name,
      String description,
      String status,
      String ownerTeam,
      Instant createdAt,
      String createdBy
  ) {}

  public record CreateApplicationDto(
      String slug,
      String name,
      String description,
      String ownerTeam
  ) {}

  public record UpdateApplicationDto(
      String name,
      String description,
      String ownerTeam
  ) {}

  public record ApiKeyDto(
      UUID id,
      String keyPrefix,
      String name,
      List<String> scopes,
      Instant createdAt,
      Instant expiresAt,
      Instant revokedAt
  ) {}

  public record CreateApiKeyDto(
      String name,
      List<String> scopes,
      Instant expiresAt
  ) {}

  public record ApiKeyCreatedDto(
      UUID id,
      String keyPrefix,
      String apiKey,
      List<String> scopes,
      Instant expiresAt,
      Instant createdAt
  ) {}
}
