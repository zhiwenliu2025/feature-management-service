package com.fms.security;

import jakarta.servlet.http.HttpServletRequest;

final class DataPlaneScopeResolver {

    private DataPlaneScopeResolver() {
    }

    static String requiredScope(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.startsWith("/api/v1/sync")) {
            return "sync";
        }
        if (path.startsWith("/api/v1/evaluate")) {
            return "evaluate";
        }
        if (path.startsWith("/api/v1/explain")) {
            return "explain:read";
        }
        return null;
    }
}
