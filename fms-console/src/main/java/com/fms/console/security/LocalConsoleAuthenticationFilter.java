package com.fms.console.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Local development only: provides an authenticated principal so Vaadin views
 * are accessible without OIDC. # SECURITY-REVIEW: local profile only.
 */
@Component
@Profile("local")
public class LocalConsoleAuthenticationFilter extends OncePerRequestFilter {

  private static final List<SimpleGrantedAuthority> LOCAL_AUTHORITIES = List.of(
      new SimpleGrantedAuthority("ROLE_admin"),
      new SimpleGrantedAuthority("SCOPE_flags:read"),
      new SimpleGrantedAuthority("SCOPE_flags:write"),
      new SimpleGrantedAuthority("SCOPE_flags:publish"),
      new SimpleGrantedAuthority("SCOPE_flags:kill"),
      new SimpleGrantedAuthority("SCOPE_audit:read"),
      new SimpleGrantedAuthority("SCOPE_explain:read"));

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    if (isConsoleRequest(request)
        && (SecurityContextHolder.getContext().getAuthentication() == null
            || !SecurityContextHolder.getContext().getAuthentication().isAuthenticated())) {
      var authentication = new UsernamePasswordAuthenticationToken(
          "local-dev", null, LOCAL_AUTHORITIES);
      SecurityContextHolder.getContext().setAuthentication(authentication);
    }
    filterChain.doFilter(request, response);
  }

  private boolean isConsoleRequest(HttpServletRequest request) {
    String uri = request.getRequestURI();
    return !uri.startsWith("/api/")
        && !uri.startsWith("/actuator/")
        && !uri.startsWith("/swagger-ui")
        && !uri.startsWith("/v3/api-docs");
  }
}
