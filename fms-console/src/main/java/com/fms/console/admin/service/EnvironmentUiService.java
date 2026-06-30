package com.fms.console.admin.service;

import com.fms.console.client.ManagementApiClient;
import com.fms.console.client.dto.EnvironmentDtos.EnvironmentConfigDto;
import com.fms.console.client.dto.EnvironmentDtos.EnvironmentDto;
import com.fms.console.client.dto.EnvironmentDtos.PromoteDto;
import com.fms.console.client.dto.EnvironmentDtos.PromoteResultDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EnvironmentUiService {

  private final ManagementApiClient api;

  public EnvironmentUiService(ManagementApiClient api) {
    this.api = api;
  }

  public List<EnvironmentDto> list() {
    return api.listEnvironments();
  }

  public EnvironmentConfigDto getConfig(String environment) {
    return api.getEnvironmentConfig(environment);
  }

  public PromoteResultDto promote(String targetEnvironment, PromoteDto request) {
    return api.promote(targetEnvironment, request);
  }
}
