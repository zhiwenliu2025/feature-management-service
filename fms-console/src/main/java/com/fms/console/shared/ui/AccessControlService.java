package com.fms.console.shared.ui;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Maps console RBAC to UI visibility. Production wiring will delegate to
 * {@code ManagementAuthzService} once OIDC roles are integrated.
 */
@Service
public class AccessControlService {

  public boolean canReadFlags() {
    return hasAnyRole("viewer", "editor", "publisher", "kill_switch", "admin");
  }

  public boolean canWriteFlags() {
    return hasAnyRole("editor", "publisher", "admin");
  }

  public boolean canPublish() {
    return hasAnyRole("publisher", "admin");
  }

  public boolean canKillSwitch() {
    return hasAnyRole("kill_switch", "admin");
  }

  public boolean canReadAudit() {
    return hasAnyRole("viewer", "editor", "publisher", "kill_switch", "admin");
  }

  public boolean canExplain() {
    return hasAnyRole("admin") || hasAuthority("SCOPE_explain:read");
  }

  public boolean isAdmin() {
    return hasAnyRole("admin");
  }

  private boolean hasAnyRole(String... roles) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      return true;
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
