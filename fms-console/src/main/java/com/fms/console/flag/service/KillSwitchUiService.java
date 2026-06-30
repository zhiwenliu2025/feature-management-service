package com.fms.console.flag.service;

import com.fms.console.client.ManagementApiClient;
import com.fms.console.client.dto.KillSwitchDtos.KillSwitchDto;
import com.fms.console.client.dto.KillSwitchDtos.KillSwitchListDto;
import com.fms.console.client.dto.KillSwitchDtos.KillSwitchRequestDto;
import org.springframework.stereotype.Service;

@Service
public class KillSwitchUiService {

  private final ManagementApiClient api;

  public KillSwitchUiService(ManagementApiClient api) {
    this.api = api;
  }

  public KillSwitchDto activate(String flagKey, KillSwitchRequestDto request) {
    return api.activateKillSwitch(flagKey, request);
  }

  public KillSwitchDto deactivate(String flagKey, KillSwitchRequestDto request) {
    return api.deactivateKillSwitch(flagKey, request);
  }

  public KillSwitchListDto listActive(String appId, String flagKey, String environment) {
    return api.listKillSwitches(appId, flagKey, environment);
  }
}
