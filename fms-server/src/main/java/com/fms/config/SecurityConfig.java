package com.fms.config;

import com.fms.security.ApiKeyAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

  @Bean
  @Order(1)
  @Profile("local")
  SecurityFilterChain localSecurityFilterChain(
      HttpSecurity http,
      ApiKeyAuthenticationFilter apiKeyAuthenticationFilter) throws Exception {
    return http
        .securityMatcher("/api/**")
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  @Bean
  @Order(1)
  @Profile("!local")
  SecurityFilterChain productionSecurityFilterChain(
      HttpSecurity http,
      ApiKeyAuthenticationFilter apiKeyAuthenticationFilter,
      @Value("${fms.security.oauth2.enabled:true}") boolean oauth2Enabled) throws Exception {

    http
        .securityMatcher("/api/**")
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

    if (oauth2Enabled) {
      http.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
    }

    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/api/health",
                "/api/ready",
                "/api/v1/openapi.json",
                "/swagger-ui/**",
                "/v3/api-docs/**")
            .permitAll()
            .requestMatchers("/api/v1/management/**").authenticated()
            .requestMatchers("/api/v1/sync/**", "/api/v1/evaluate/**", "/api/v1/explain/**")
            .authenticated())
        .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  @Order(0)
  SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
    return http
        .securityMatcher("/actuator/**")
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .build();
  }
}
