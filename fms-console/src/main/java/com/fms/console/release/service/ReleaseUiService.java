package com.fms.console.release.service;

import com.fms.common.api.PageResponse;
import com.fms.console.client.ManagementApiClient;
import com.fms.console.client.dto.ReleaseDtos.CreateReleaseDto;
import com.fms.console.client.dto.ReleaseDtos.LinkFlagsDto;
import com.fms.console.client.dto.ReleaseDtos.ReleaseDetailDto;
import com.fms.console.client.dto.ReleaseDtos.ReleaseSummaryDto;
import org.springframework.stereotype.Service;

@Service
public class ReleaseUiService {

  private final ManagementApiClient api;

  public ReleaseUiService(ManagementApiClient api) {
    this.api = api;
  }

  public ReleaseSummaryDto create(CreateReleaseDto request) {
    return api.createRelease(request);
  }

  public PageResponse<ReleaseSummaryDto> list(int limit) {
    return api.listReleases(limit);
  }

  public ReleaseDetailDto get(String releaseId) {
    return api.getRelease(releaseId);
  }

  public ReleaseDetailDto linkFlags(String releaseId, LinkFlagsDto request) {
    return api.linkFlags(releaseId, request);
  }
}
