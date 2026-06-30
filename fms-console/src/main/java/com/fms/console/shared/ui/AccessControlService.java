package com.fms.console.shared.ui;

import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
public class AccessControlService {

  private static final Map<String, Set<String>> ROLE_SCOPES = Map.of(
      "viewer", Set.of("flags:read", "audit:read"),
      "editor", Set.of("flags:read", "flags:write", "audit:read"),
      "publisher", Set.of("flags:read", "flags:write", "flags:publish", "audit:read"),
      "kill_switch", Set.of("flags:read", "flags:kill", "audit:read"),
      "admin", Set.of("*"));

  private final boolean localProfile;

  public AccessControlService(Environment environment) {
    this.localProfile = Arrays.asList(environment.getActiveProfiles()).contains("local");
  }

  public boolean canReadFlags() {
    return hasScope("flags:read");
  }

  public boolean canWriteFlags() {
    return hasScope("flags:write");
  }

  public boolean canPublish() {
    return hasScope("flags:publish");
  }

  public boolean canKillSwitch() {
    return hasScope("flags:kill");
  }

  public boolean canReadAudit() {
    return hasScope("audit:read");
  }

  public boolean canExplain() {
    return hasScope("explain:read") || isAdmin();
  }

  public boolean canExplainPii() {
    return hasScope("explain:pii");
  }

  public boolean isAdmin() {
    return hasScope("admin") || hasAnyRole("admin");
  }

  private boolean hasScope(String scope) {
    if (localProfile) {
      return true;
    }
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      return false;
    }
    if ("admin".equals(scope) && hasAnyRole("admin")) {
      return true;
    }
    Set<String> granted = resolveScopes(authentication);
    return granted.contains("*") || granted.contains(scope);
  }

  private Set<String> resolveScopes(Authentication authentication) {
    Set<String> scopes = new HashSet<>();
    for (GrantedAuthority authority : authentication.getAuthorities()) {
      String value = authority.getAuthority();
      if (value.startsWith("SCOPE_")) {
        scopes.add(value.substring("SCOPE_".length()));
      }
      if (value.startsWith("ROLE_")) {
        String role = value.substring("ROLE_".length());
        Set<String> mapped = ROLE_SCOPES.get(role);
        if (mapped != null) {
          scopes.addAll(mapped);
        }
      }
      if (ROLE_SCOPES.containsKey(value)) {
        scopes.addAll(ROLE_SCOPES.get(value));
      }
    }
    return scopes;
  }

  private boolean hasAnyRole(String... roles) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      return false;
    }
    for (String role : roles) {
      if (hasAuthority("ROLE_" + role) || hasAuthority(role)) {
        return true;
      }
    }
    return false;
  }

  private boolean hasAuthority(String authority) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      return false;
    }
    return authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch(authority::equals);
  }
}
