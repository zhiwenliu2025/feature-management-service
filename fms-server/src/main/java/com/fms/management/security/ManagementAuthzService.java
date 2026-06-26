package com.fms.management.security;

import com.fms.common.exception.FmsErrorCode;
import com.fms.common.exception.FmsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ManagementAuthzService {

    private static final Map<String, Set<String>> ROLE_SCOPES = Map.of(
            "viewer", Set.of("flags:read", "audit:read"),
            "editor", Set.of("flags:read", "flags:write", "audit:read"),
            "publisher", Set.of("flags:read", "flags:write", "flags:publish", "audit:read"),
            "kill_switch", Set.of("flags:read", "flags:kill", "audit:read"),
            "admin", Set.of("*"));

    public void checkScope(String scope) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new FmsException(FmsErrorCode.UNAUTHORIZED, "Authentication required.");
        }
        if (!(auth.getPrincipal() instanceof Jwt)) {
            return;
        }
        Set<String> granted = resolveScopes((Jwt) auth.getPrincipal());
        Jwt jwt = (Jwt) auth.getPrincipal();
        if ("admin".equals(scope) && hasAdminRole(jwt)) {
            return;
        }
        if (granted.contains("*") || granted.contains(scope)) {
            return;
        }
        throw new FmsException(FmsErrorCode.FORBIDDEN, "Insufficient scope for this operation.");
    }

    private boolean hasAdminRole(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null && roles.contains("admin");
    }

    private Set<String> resolveScopes(Jwt jwt) {
        Set<String> scopes = new HashSet<>();
        String scopeClaim = jwt.getClaimAsString("scope");
        if (scopeClaim != null) {
            scopes.addAll(Arrays.asList(scopeClaim.split(" ")));
        }
        List<String> scp = jwt.getClaimAsStringList("scp");
        if (scp != null) {
            scopes.addAll(scp);
        }
        List<String> permissions = jwt.getClaimAsStringList("permissions");
        if (permissions != null) {
            scopes.addAll(permissions);
        }
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles != null) {
            for (String role : roles) {
                Set<String> mapped = ROLE_SCOPES.get(role);
                if (mapped != null) {
                    scopes.addAll(mapped);
                }
            }
        }
        return scopes;
    }
}
