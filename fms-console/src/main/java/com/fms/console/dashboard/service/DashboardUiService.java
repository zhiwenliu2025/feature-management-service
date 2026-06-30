package com.fms.console.dashboard.service;

import com.fms.common.api.PageResponse;
import com.fms.console.admin.service.EnvironmentUiService;
import com.fms.console.audit.service.AuditUiService;
import com.fms.console.client.dto.AuditDtos.AuditEventDto;
import com.fms.console.client.dto.EnvironmentDtos.EnvironmentConfigDto;
import com.fms.console.client.dto.FlagDtos.FlagSummaryDto;
import com.fms.console.flag.service.FlagUiService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DashboardUiService {

  private final FlagUiService flagUiService;
  private final AuditUiService auditUiService;
  private final EnvironmentUiService environmentUiService;

  public DashboardUiService(
      FlagUiService flagUiService,
      AuditUiService auditUiService,
      EnvironmentUiService environmentUiService) {
    this.flagUiService = flagUiService;
    this.auditUiService = auditUiService;
    this.environmentUiService = environmentUiService;
  }

  public DashboardSnapshot load(String appId, String environment) {
    PageResponse<FlagSummaryDto> all = flagUiService.listFlags(appId, null, null, null, 100, null);
    List<FlagSummaryDto> flags = all.data();
    long total = all.pagination().totalCount() != null ? all.pagination().totalCount() : flags.size();
    long published = flags.stream().filter(f -> "published".equalsIgnoreCase(f.status())).count();
    long draft = flags.stream().filter(f -> "draft".equalsIgnoreCase(f.status())).count();
    List<FlagSummaryDto> dirty = flags.stream().filter(FlagSummaryDto::draftDirty).toList();
    PageResponse<AuditEventDto> audit = auditUiService.query(
        null, null, null, null, environment, null, null, 10, null);
    EnvironmentConfigDto config = environmentUiService.getConfig(environment);
    return new DashboardSnapshot(total, published, draft, 0, dirty, audit.data(), config);
  }

  public record DashboardSnapshot(
      long totalFlags,
      long publishedFlags,
      long draftFlags,
      long activeKillSwitches,
      List<FlagSummaryDto> draftDirtyFlags,
      List<AuditEventDto> recentAudit,
      EnvironmentConfigDto environmentConfig
  ) {}
}
