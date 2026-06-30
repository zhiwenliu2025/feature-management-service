package com.fms.console.security;

import com.fms.console.shared.ui.LoginView;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@EnableWebSecurity
public class ConsoleSecurityConfig {

  private static final RequestMatcher CONSOLE_MATCHER = request -> {
    String uri = request.getRequestURI();
    return !uri.startsWith("/api/")
        && !uri.startsWith("/actuator/")
        && !uri.startsWith("/swagger-ui")
        && !uri.startsWith("/v3/api-docs");
  };

  @Bean
  @Order(2)
  @Profile("local")
  SecurityFilterChain localConsoleSecurityFilterChain(
      HttpSecurity http,
      LocalConsoleAuthenticationFilter localConsoleAuthenticationFilter) throws Exception {
    return http
        .securityMatcher(CONSOLE_MATCHER)
        .addFilterBefore(localConsoleAuthenticationFilter,
            org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
        .with(VaadinSecurityConfigurer.vaadin(), configurer -> {})
        .build();
  }

  @Bean
  @Order(2)
  @Profile("!local")
  SecurityFilterChain consoleSecurityFilterChain(HttpSecurity http) throws Exception {
    return http
        .securityMatcher(CONSOLE_MATCHER)
        .with(VaadinSecurityConfigurer.vaadin(), configurer -> configurer.loginView(LoginView.class))
        .build();
  }
}
