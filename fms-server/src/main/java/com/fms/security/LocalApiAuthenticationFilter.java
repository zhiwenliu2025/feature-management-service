package com.fms.security;

import com.fms.config.FmsSecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * Local development only: authenticates API calls when API-key enforcement is disabled so the
 * Vaadin console can invoke data-plane endpoints without configuring a key.
 * # SECURITY-REVIEW: local profile only; skipped when api-key enforcement is enabled.
 */
public class LocalApiAuthenticationFilter extends OncePerRequestFilter {

  private static final UUID LOCAL_KEY_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

  private static final ApiKeyPrincipal LOCAL_PRINCIPAL = new ApiKeyPrincipal(
      LOCAL_KEY_ID,
      "checkout-service",
      Set.of("sync", "evaluate", "explain:read", "explain:pii"));

  private final FmsSecurityProperties securityProperties;

  public LocalApiAuthenticationFilter(FmsSecurityProperties securityProperties) {
    this.securityProperties = securityProperties;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    if (securityProperties.localApiAutoAuth()
        && isApiRequest(request)
        && !isAuthenticated()) {
      var authorities = LOCAL_PRINCIPAL.scopes().stream()
          .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
          .toList();
      var authentication = new UsernamePasswordAuthenticationToken(LOCAL_PRINCIPAL, null, authorities);
      SecurityContextHolder.getContext().setAuthentication(authentication);
    }
    filterChain.doFilter(request, response);
  }

  private boolean isApiRequest(HttpServletRequest request) {
    return request.getRequestURI().startsWith("/api/");
  }

  private boolean isAuthenticated() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null && authentication.isAuthenticated();
  }
}
