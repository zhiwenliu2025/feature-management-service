package com.fms.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authenticates data-plane requests via {@code Authorization: ApiKey <key>} header.
 * # SECURITY-REVIEW: Production must validate key hash against api_keys table.
 */
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String API_KEY_PREFIX = "ApiKey ";
    private static final List<String> DATA_PLANE_PREFIXES = List.of(
            "/api/v1/sync",
            "/api/v1/evaluate",
            "/api/v1/explain");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (!isDataPlaneRequest(request) || SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith(API_KEY_PREFIX)) {
            String apiKey = authorization.substring(API_KEY_PREFIX.length()).trim();
            if (!apiKey.isEmpty()) {
                // Scaffold: accept any non-empty key; replace with hashed lookup
                var auth = new UsernamePasswordAuthenticationToken(
                        "api-key-client",
                        null,
                        List.of(new SimpleGrantedAuthority("SCOPE_sync"), new SimpleGrantedAuthority("SCOPE_evaluate")));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isDataPlaneRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        return DATA_PLANE_PREFIXES.stream().anyMatch(path::startsWith);
    }
}
