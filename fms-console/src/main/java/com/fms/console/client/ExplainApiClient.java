package com.fms.console.client;

import com.fms.console.client.dto.ExplainDtos.ExplainRequestDto;
import com.fms.console.client.dto.ExplainDtos.ExplainResponseDto;
import com.fms.console.client.dto.ExplainDtos.ReplayExplainRequestDto;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ExplainApiClient {

  private final RestClient restClient;

  public ExplainApiClient(RestClient managementRestClient) {
    this.restClient = managementRestClient;
  }

  public ExplainResponseDto explain(String flagKey, ExplainRequestDto request) {
    return restClient.post()
        .uri("/v1/explain/flags/{flagKey}", flagKey)
        .body(request)
        .retrieve()
        .body(ExplainResponseDto.class);
  }

  public ExplainResponseDto replay(String flagKey, ReplayExplainRequestDto request) {
    return restClient.post()
        .uri("/v1/explain/flags/{flagKey}/replay", flagKey)
        .body(request)
        .retrieve()
        .body(ExplainResponseDto.class);
  }
}
