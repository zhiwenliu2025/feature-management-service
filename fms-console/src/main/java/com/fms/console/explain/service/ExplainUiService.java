package com.fms.console.explain.service;

import com.fms.console.client.ExplainApiClient;
import com.fms.console.client.dto.ExplainDtos.ExplainRequestDto;
import com.fms.console.client.dto.ExplainDtos.ExplainResponseDto;
import com.fms.console.client.dto.ExplainDtos.ReplayExplainRequestDto;
import org.springframework.stereotype.Service;

@Service
public class ExplainUiService {

  private final ExplainApiClient api;

  public ExplainUiService(ExplainApiClient api) {
    this.api = api;
  }

  public ExplainResponseDto explain(String flagKey, ExplainRequestDto request) {
    return api.explain(flagKey, request);
  }

  public ExplainResponseDto replay(String flagKey, ReplayExplainRequestDto request) {
    return api.replay(flagKey, request);
  }
}
