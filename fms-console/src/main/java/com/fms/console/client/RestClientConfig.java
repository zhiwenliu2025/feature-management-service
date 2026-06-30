package com.fms.console.client;

import tools.jackson.databind.ObjectMapper;
import com.fms.common.api.ErrorResponse;
import com.fms.console.config.FmsConsoleProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(FmsConsoleProperties.class)
public class RestClientConfig {

  @Bean
  RestClient managementRestClient(
      FmsConsoleProperties properties,
      ObjectMapper objectMapper,
      @Value("${server.port:8080}") int serverPort) {
    String baseUrl = properties.apiBaseUrl();
    if (baseUrl.isBlank()) {
      baseUrl = "http://127.0.0.1:" + serverPort + "/api";
    }

    return RestClient.builder()
        .baseUrl(baseUrl)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .requestInterceptor((request, body, execution) -> {
          applyAuthHeaders(request.getHeaders(), properties);
          return execution.execute(request, body);
        })
        .defaultStatusHandler(status -> status.is4xxClientError() || status.is5xxServerError(), (request, response) -> {
          String message = "Request failed.";
          String errorCode = "ERROR";
          try {
            ErrorResponse error = objectMapper.readValue(response.getBody().readAllBytes(), ErrorResponse.class);
            if (error.error() != null) {
              if (error.error().message() != null) {
                message = error.error().message();
              }
              if (error.error().code() != null) {
                errorCode = error.error().code();
              }
            }
          } catch (Exception ignored) {
            // use generic message
          }
          throw new FmsUiException(response.getStatusCode().value(), errorCode, message);
        })
        .build();
  }

  private void applyAuthHeaders(HttpHeaders headers, FmsConsoleProperties properties) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof JwtAuthenticationToken jwtAuth) {
      Jwt jwt = jwtAuth.getToken();
      headers.setBearerAuth(jwt.getTokenValue());
      return;
    }
    if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
      headers.setBearerAuth(jwt.getTokenValue());
      return;
    }
    if (!properties.localApiKey().isBlank()) {
      headers.set(HttpHeaders.AUTHORIZATION, "ApiKey " + properties.localApiKey());
    }
  }
}
