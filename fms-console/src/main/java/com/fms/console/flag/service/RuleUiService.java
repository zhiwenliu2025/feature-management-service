package com.fms.console.flag.service;

import com.fms.console.client.ManagementApiClient;
import com.fms.console.client.dto.FlagDtos.FlagDetailDto;
import com.fms.console.client.dto.RuleDtos.ReplaceRulesDto;
import com.fms.console.client.dto.RuleDtos.UpdateRuleDto;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RuleUiService {

  private final ManagementApiClient api;

  public RuleUiService(ManagementApiClient api) {
    this.api = api;
  }

  public FlagDetailDto replaceRules(String appId, String flagKey, ReplaceRulesDto request) {
    return api.replaceRules(appId, flagKey, request);
  }

  public FlagDetailDto updateRule(
      String appId, String flagKey, UUID ruleId, String environment, UpdateRuleDto request) {
    return api.updateRule(appId, flagKey, ruleId, environment, request);
  }
}
