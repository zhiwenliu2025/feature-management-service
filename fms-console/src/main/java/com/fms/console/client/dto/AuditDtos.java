package com.fms.console.client.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AuditDtos {

  private AuditDtos() {}

  public record AuditEventDto(
      String id,
      String actor,
      String action,
      String resourceType,
      String resourceId,
      String environment,
      Map<String, Object> diff,
      Map<String, Object> metadata,
      Instant createdAt
  ) {}
}
