package com.fms.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fms.common.api.ErrorResponse;
import com.fms.common.exception.FmsErrorCode;
import com.fms.config.FmsSecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * Authenticates data-plane requests via {@code Authorization: ApiKey <key>} header.
 * # SECURITY-REVIEW: Validates key hash against api_keys table and enforces scopes.
 */
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String API_KEY_PREFIX = "ApiKey ";
    private static final List<String> DATA_PLANE_PREFIXES = List.of(
            "/api/v1/sync",
            "/api/v1/evaluate",
            "/api/v1/explain");

    private final ApiKeyAuthenticationService apiKeyAuthenticationService;
    private final FmsSecurityProperties securityProperties;
    private final ObjectMapper objectMapper;

    public ApiKeyAuthenticationFilter(
            ApiKeyAuthenticationService apiKeyAuthenticationService,
            FmsSecurityProperties securityProperties,
            ObjectMapper objectMapper) {
        this.apiKeyAuthenticationService = apiKeyAuthenticationService;
        this.securityProperties = securityProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (!isDataPlaneRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith(API_KEY_PREFIX)) {
            String apiKey = authorization.substring(API_KEY_PREFIX.length()).trim();
            Optional<ApiKeyPrincipal> principal = apiKeyAuthenticationService.authenticate(apiKey);
            if (principal.isEmpty()) {
                writeError(response, request, FmsErrorCode.UNAUTHORIZED, "Invalid API key.");
                return;
            }

            String requiredScope = DataPlaneScopeResolver.requiredScope(request);
            if (requiredScope != null && !principal.get().hasScope(requiredScope)) {
                writeError(response, request, FmsErrorCode.FORBIDDEN, "Insufficient scope for this operation.");
                return;
            }

            var authorities = principal.get().scopes().stream()
                    .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                    .toList();
            var auth = new UsernamePasswordAuthenticationToken(principal.get(), null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
            filterChain.doFilter(request, response);
            return;
        }

        if (securityProperties.apiKey().enforced()) {
            writeError(response, request, FmsErrorCode.UNAUTHORIZED, "Authentication required.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeError(
            HttpServletResponse response,
            HttpServletRequest request,
            FmsErrorCode errorCode,
            String message) throws IOException {
        response.setStatus(errorCode.httpStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ErrorResponse body = ErrorResponse.of(
                errorCode.name(),
                message,
                request.getHeader("X-Request-Id"),
                null);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private boolean isDataPlaneRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        return DATA_PLANE_PREFIXES.stream().anyMatch(path::startsWith);
    }
}
