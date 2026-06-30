package com.fms.console.flag.service;

import com.fms.common.api.PageResponse;
import com.fms.console.client.ManagementApiClient;
import com.fms.console.client.dto.FlagDtos.CreateFlagDto;
import com.fms.console.client.dto.FlagDtos.FlagDetailDto;
import com.fms.console.client.dto.FlagDtos.FlagSummaryDto;
import com.fms.console.client.dto.FlagDtos.FlagVersionDetailDto;
import com.fms.console.client.dto.FlagDtos.FlagVersionSummaryDto;
import com.fms.console.client.dto.FlagDtos.PublishFlagDto;
import com.fms.console.client.dto.FlagDtos.PublishFlagResultDto;
import com.fms.console.client.dto.FlagDtos.RollbackFlagDto;
import com.fms.console.client.dto.FlagDtos.UpdateFlagDto;
import org.springframework.stereotype.Service;

@Service
public class FlagUiService {

  private final ManagementApiClient api;

  public FlagUiService(ManagementApiClient api) {
    this.api = api;
  }

  public PageResponse<FlagSummaryDto> listFlags(
      String appId, String status, String tag, String search, int limit, String cursor) {
    return api.listFlags(appId, status, tag, search, limit, cursor);
  }

  public FlagDetailDto getFlag(String appId, String flagKey) {
    return api.getFlag(appId, flagKey);
  }

  public FlagDetailDto createFlag(CreateFlagDto request) {
    return api.createFlag(request);
  }

  public FlagDetailDto updateFlag(String appId, String flagKey, UpdateFlagDto request) {
    return api.updateFlag(appId, flagKey, request);
  }

  public void archiveFlag(String appId, String flagKey) {
    api.archiveFlag(appId, flagKey);
  }

  public PublishFlagResultDto publish(String appId, String flagKey, PublishFlagDto request) {
    return api.publishFlag(appId, flagKey, request);
  }

  public PublishFlagResultDto rollback(String flagKey, RollbackFlagDto request) {
    return api.rollbackFlag(flagKey, request);
  }

  public PageResponse<FlagVersionSummaryDto> listVersions(
      String appId, String flagKey, String environment, int limit) {
    return api.listVersions(appId, flagKey, environment, limit);
  }

  public FlagVersionDetailDto getVersion(String appId, String flagKey, int version, String environment) {
    return api.getVersion(appId, flagKey, version, environment);
  }
}
