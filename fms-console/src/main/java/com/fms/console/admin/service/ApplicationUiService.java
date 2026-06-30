package com.fms.console.admin.service;

import com.fms.common.api.PageResponse;
import com.fms.console.client.ManagementApiClient;
import com.fms.console.client.dto.ApplicationDtos.ApiKeyCreatedDto;
import com.fms.console.client.dto.ApplicationDtos.ApiKeyDto;
import com.fms.console.client.dto.ApplicationDtos.ApplicationDto;
import com.fms.console.client.dto.ApplicationDtos.CreateApiKeyDto;
import com.fms.console.client.dto.ApplicationDtos.CreateApplicationDto;
import com.fms.console.client.dto.ApplicationDtos.UpdateApplicationDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ApplicationUiService {

  private final ManagementApiClient api;

  public ApplicationUiService(ManagementApiClient api) {
    this.api = api;
  }

  public ApplicationDto create(CreateApplicationDto request) {
    return api.createApplication(request);
  }

  public PageResponse<ApplicationDto> list(int limit, String cursor) {
    return api.listApplications(limit, cursor);
  }

  public ApplicationDto get(String appId) {
    return api.getApplication(appId);
  }

  public ApplicationDto update(String appId, UpdateApplicationDto request) {
    return api.updateApplication(appId, request);
  }

  public ApiKeyCreatedDto createApiKey(String appId, CreateApiKeyDto request) {
    return api.createApiKey(appId, request);
  }

  public List<ApiKeyDto> listApiKeys(String appId) {
    return api.listApiKeys(appId);
  }

  public void revokeApiKey(String appId, UUID keyId) {
    api.revokeApiKey(appId, keyId);
  }
}
