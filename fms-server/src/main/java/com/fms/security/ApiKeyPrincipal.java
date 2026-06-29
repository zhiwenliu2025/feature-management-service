package com.fms.security;

import java.util.Set;
import java.util.UUID;

public record ApiKeyPrincipal(UUID keyId, String appId, Set<String> scopes) {

    public boolean hasScope(String scope) {
        return scopes.contains(scope);
    }
}
