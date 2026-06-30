package com.fms.console.client.dto;

import java.time.Instant;
import java.util.List;

public final class KillSwitchDtos {

  private KillSwitchDtos() {}

  public record KillSwitchRequestDto(
      String appId,
      String environment,
      String scope,
      String regionCode,
      Object forcedValue,
      String comment
  ) {}

  public record KillSwitchDto(
      String flagKey,
      String environment,
      String scope,
      String regionCode,
      boolean isActive,
      Object forcedValue,
      Instant activatedAt,
      String activatedBy,
      Long configVersion
  ) {}

  public record KillSwitchListDto(
      List<KillSwitchDto> overrides
  ) {}
}
