package com.fms.console.audit.service;

import com.fms.common.api.PageResponse;
import com.fms.console.client.ManagementApiClient;
import com.fms.console.client.dto.AuditDtos.AuditEventDto;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuditUiService {

  private final ManagementApiClient api;

  public AuditUiService(ManagementApiClient api) {
    this.api = api;
  }

  public PageResponse<AuditEventDto> query(
      String resourceType,
      String resourceId,
      String actor,
      String action,
      String environment,
      Instant from,
      Instant to,
      int limit,
      String cursor) {
    return api.queryAudit(resourceType, resourceId, actor, action, environment, from, to, limit, cursor);
  }
}
