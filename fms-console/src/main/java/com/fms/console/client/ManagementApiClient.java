package com.fms.console.client;

import com.fms.common.api.PageResponse;
import com.fms.console.client.dto.ApplicationDtos.ApiKeyCreatedDto;
import com.fms.console.client.dto.ApplicationDtos.ApiKeyDto;
import com.fms.console.client.dto.ApplicationDtos.ApplicationDto;
import com.fms.console.client.dto.ApplicationDtos.CreateApiKeyDto;
import com.fms.console.client.dto.ApplicationDtos.CreateApplicationDto;
import com.fms.console.client.dto.ApplicationDtos.UpdateApplicationDto;
import com.fms.console.client.dto.AuditDtos.AuditEventDto;
import com.fms.console.client.dto.EnvironmentDtos.EnvironmentConfigDto;
import com.fms.console.client.dto.EnvironmentDtos.EnvironmentDto;
import com.fms.console.client.dto.EnvironmentDtos.PromoteDto;
import com.fms.console.client.dto.EnvironmentDtos.PromoteResultDto;
import com.fms.console.client.dto.FlagDtos.CreateFlagDto;
import com.fms.console.client.dto.FlagDtos.FlagDetailDto;
import com.fms.console.client.dto.FlagDtos.FlagSummaryDto;
import com.fms.console.client.dto.FlagDtos.FlagVersionDetailDto;
import com.fms.console.client.dto.FlagDtos.FlagVersionSummaryDto;
import com.fms.console.client.dto.FlagDtos.PublishFlagDto;
import com.fms.console.client.dto.FlagDtos.PublishFlagResultDto;
import com.fms.console.client.dto.FlagDtos.RollbackFlagDto;
import com.fms.console.client.dto.FlagDtos.UpdateFlagDto;
import com.fms.console.client.dto.KillSwitchDtos.KillSwitchDto;
import com.fms.console.client.dto.KillSwitchDtos.KillSwitchListDto;
import com.fms.console.client.dto.KillSwitchDtos.KillSwitchRequestDto;
import com.fms.console.client.dto.ReleaseDtos.CreateReleaseDto;
import com.fms.console.client.dto.ReleaseDtos.LinkFlagsDto;
import com.fms.console.client.dto.ReleaseDtos.ReleaseDetailDto;
import com.fms.console.client.dto.ReleaseDtos.ReleaseSummaryDto;
import com.fms.console.client.dto.RuleDtos.ReplaceRulesDto;
import com.fms.console.client.dto.RuleDtos.UpdateRuleDto;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class ManagementApiClient {

  private final RestClient restClient;

  public ManagementApiClient(RestClient managementRestClient) {
    this.restClient = managementRestClient;
  }

  // --- Flags ---

  public PageResponse<FlagSummaryDto> listFlags(
      String appId, String status, String tag, String search, int limit, String cursor) {
    return restClient.get()
        .uri(uri -> uri.path("/v1/management/flags")
            .queryParam("appId", appId)
            .queryParamIfPresent("status", java.util.Optional.ofNullable(status))
            .queryParamIfPresent("tag", java.util.Optional.ofNullable(tag))
            .queryParamIfPresent("search", java.util.Optional.ofNullable(search))
            .queryParam("limit", limit)
            .queryParamIfPresent("cursor", java.util.Optional.ofNullable(cursor))
            .build())
        .retrieve()
        .body(new ParameterizedTypeReference<>() {});
  }

  public FlagDetailDto getFlag(String appId, String flagKey) {
    return restClient.get()
        .uri("/v1/management/flags/{flagKey}?appId={appId}", flagKey, appId)
        .retrieve()
        .body(FlagDetailDto.class);
  }

  public FlagDetailDto createFlag(CreateFlagDto request) {
    return restClient.post()
        .uri("/v1/management/flags")
        .body(request)
        .retrieve()
        .body(FlagDetailDto.class);
  }

  public FlagDetailDto updateFlag(String appId, String flagKey, UpdateFlagDto request) {
    return restClient.put()
        .uri("/v1/management/flags/{flagKey}?appId={appId}", flagKey, appId)
        .body(request)
        .retrieve()
        .body(FlagDetailDto.class);
  }

  public void archiveFlag(String appId, String flagKey) {
    restClient.delete()
        .uri("/v1/management/flags/{flagKey}?appId={appId}", flagKey, appId)
        .retrieve()
        .toBodilessEntity();
  }

  public PublishFlagResultDto publishFlag(String appId, String flagKey, PublishFlagDto request) {
    return restClient.post()
        .uri("/v1/management/flags/{flagKey}/publish?appId={appId}", flagKey, appId)
        .body(request)
        .retrieve()
        .body(PublishFlagResultDto.class);
  }

  public PublishFlagResultDto rollbackFlag(String flagKey, RollbackFlagDto request) {
    return restClient.post()
        .uri("/v1/management/flags/{flagKey}/rollback")
        .body(request)
        .retrieve()
        .body(PublishFlagResultDto.class);
  }

  public PageResponse<FlagVersionSummaryDto> listVersions(
      String appId, String flagKey, String environment, int limit) {
    return restClient.get()
        .uri("/v1/management/flags/{flagKey}/versions?appId={appId}&environment={env}&limit={limit}",
            flagKey, appId, environment, limit)
        .retrieve()
        .body(new ParameterizedTypeReference<>() {});
  }

  public FlagVersionDetailDto getVersion(String appId, String flagKey, int version, String environment) {
    return restClient.get()
        .uri("/v1/management/flags/{flagKey}/versions/{version}?appId={appId}&environment={env}",
            flagKey, version, appId, environment)
        .retrieve()
        .body(FlagVersionDetailDto.class);
  }

  // --- Rules ---

  public FlagDetailDto replaceRules(String appId, String flagKey, ReplaceRulesDto request) {
    return restClient.put()
        .uri("/v1/management/flags/{flagKey}/rules?appId={appId}", flagKey, appId)
        .body(request)
        .retrieve()
        .body(FlagDetailDto.class);
  }

  public FlagDetailDto updateRule(
      String appId, String flagKey, UUID ruleId, String environment, UpdateRuleDto request) {
    return restClient.patch()
        .uri("/v1/management/flags/{flagKey}/rules/{ruleId}?appId={appId}&environment={env}",
            flagKey, ruleId, appId, environment)
        .body(request)
        .retrieve()
        .body(FlagDetailDto.class);
  }

  // --- Kill switch ---

  public KillSwitchDto activateKillSwitch(String flagKey, KillSwitchRequestDto request) {
    return restClient.post()
        .uri("/v1/management/flags/{flagKey}/kill-switch", flagKey)
        .body(request)
        .retrieve()
        .body(KillSwitchDto.class);
  }

  public KillSwitchDto deactivateKillSwitch(String flagKey, KillSwitchRequestDto request) {
    return restClient.method(org.springframework.http.HttpMethod.DELETE)
        .uri("/v1/management/flags/{flagKey}/kill-switch", flagKey)
        .body(request)
        .retrieve()
        .body(KillSwitchDto.class);
  }

  public KillSwitchListDto listKillSwitches(String appId, String flagKey, String environment) {
    return restClient.get()
        .uri("/v1/management/flags/{flagKey}/kill-switch?appId={appId}&environment={env}",
            flagKey, appId, environment)
        .retrieve()
        .body(KillSwitchListDto.class);
  }

  // --- Audit ---

  public PageResponse<AuditEventDto> queryAudit(
      String resourceType,
      String resourceId,
      String actor,
      String action,
      String environment,
      Instant from,
      Instant to,
      int limit,
      String cursor) {
    return restClient.get()
        .uri(uri -> {
          var builder = uri.path("/v1/management/audit")
              .queryParam("limit", limit);
          if (resourceType != null) {
            builder.queryParam("resourceType", resourceType);
          }
          if (resourceId != null) {
            builder.queryParam("resourceId", resourceId);
          }
          if (actor != null) {
            builder.queryParam("actor", actor);
          }
          if (action != null) {
            builder.queryParam("action", action);
          }
          if (environment != null) {
            builder.queryParam("environment", environment);
          }
          if (from != null) {
            builder.queryParam("from", from);
          }
          if (to != null) {
            builder.queryParam("to", to);
          }
          if (cursor != null) {
            builder.queryParam("cursor", cursor);
          }
          return builder.build();
        })
        .retrieve()
        .body(new ParameterizedTypeReference<>() {});
  }

  // --- Releases ---

  public ReleaseSummaryDto createRelease(CreateReleaseDto request) {
    return restClient.post()
        .uri("/v1/management/releases")
        .body(request)
        .retrieve()
        .body(ReleaseSummaryDto.class);
  }

  public PageResponse<ReleaseSummaryDto> listReleases(int limit) {
    return restClient.get()
        .uri("/v1/management/releases?limit={limit}", limit)
        .retrieve()
        .body(new ParameterizedTypeReference<>() {});
  }

  public ReleaseDetailDto getRelease(String releaseId) {
    return restClient.get()
        .uri("/v1/management/releases/{releaseId}", releaseId)
        .retrieve()
        .body(ReleaseDetailDto.class);
  }

  public ReleaseDetailDto linkFlags(String releaseId, LinkFlagsDto request) {
    return restClient.post()
        .uri("/v1/management/releases/{releaseId}/flags", releaseId)
        .body(request)
        .retrieve()
        .body(ReleaseDetailDto.class);
  }

  // --- Applications ---

  public ApplicationDto createApplication(CreateApplicationDto request) {
    return restClient.post()
        .uri("/v1/management/applications")
        .body(request)
        .retrieve()
        .body(ApplicationDto.class);
  }

  public PageResponse<ApplicationDto> listApplications(int limit, String cursor) {
    return restClient.get()
        .uri(uri -> uri.path("/v1/management/applications")
            .queryParam("limit", limit)
            .queryParamIfPresent("cursor", java.util.Optional.ofNullable(cursor))
            .build())
        .retrieve()
        .body(new ParameterizedTypeReference<>() {});
  }

  public ApplicationDto getApplication(String appId) {
    return restClient.get()
        .uri("/v1/management/applications/{appId}", appId)
        .retrieve()
        .body(ApplicationDto.class);
  }

  public ApplicationDto updateApplication(String appId, UpdateApplicationDto request) {
    return restClient.put()
        .uri("/v1/management/applications/{appId}", appId)
        .body(request)
        .retrieve()
        .body(ApplicationDto.class);
  }

  public ApiKeyCreatedDto createApiKey(String appId, CreateApiKeyDto request) {
    return restClient.post()
        .uri("/v1/management/applications/{appId}/api-keys", appId)
        .body(request)
        .retrieve()
        .body(ApiKeyCreatedDto.class);
  }

  public List<ApiKeyDto> listApiKeys(String appId) {
    return restClient.get()
        .uri("/v1/management/applications/{appId}/api-keys", appId)
        .retrieve()
        .body(new ParameterizedTypeReference<>() {});
  }

  public void revokeApiKey(String appId, UUID keyId) {
    restClient.delete()
        .uri("/v1/management/applications/{appId}/api-keys/{keyId}", appId, keyId)
        .retrieve()
        .toBodilessEntity();
  }

  // --- Environments ---

  public List<EnvironmentDto> listEnvironments() {
    return restClient.get()
        .uri("/v1/management/environments")
        .retrieve()
        .body(new ParameterizedTypeReference<>() {});
  }

  public EnvironmentConfigDto getEnvironmentConfig(String environment) {
    return restClient.get()
        .uri("/v1/management/environments/{env}/config", environment)
        .retrieve()
        .body(EnvironmentConfigDto.class);
  }

  public PromoteResultDto promote(String targetEnvironment, PromoteDto request) {
    return restClient.post()
        .uri("/v1/management/environments/{env}/promote", targetEnvironment)
        .body(request)
        .retrieve()
        .body(PromoteResultDto.class);
  }
}
