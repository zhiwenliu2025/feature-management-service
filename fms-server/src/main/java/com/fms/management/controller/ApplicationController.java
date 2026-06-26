package com.fms.management.controller;

import com.fms.common.api.PageResponse;
import com.fms.management.service.ApplicationService;
import com.fms.management.dto.ApiKeyCreatedResponse;
import com.fms.management.dto.ApiKeyResponse;
import com.fms.management.dto.ApplicationResponse;
import com.fms.management.dto.CreateApiKeyRequest;
import com.fms.management.dto.CreateApplicationRequest;
import com.fms.management.dto.UpdateApplicationRequest;
import com.fms.management.security.RequiresScope;
import com.fms.management.support.ManagementActorResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/management/applications")
@Tag(name = "Management — Applications", description = "Application onboarding and API keys")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final ManagementActorResolver actorResolver;

    public ApplicationController(ApplicationService applicationService, ManagementActorResolver actorResolver) {
        this.applicationService = applicationService;
        this.actorResolver = actorResolver;
    }

    @PostMapping
    @RequiresScope("admin")
    @Operation(summary = "Register application")
    ResponseEntity<ApplicationResponse> createApplication(
            @Valid @RequestBody CreateApplicationRequest request,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {
        String actor = actorResolver.resolve(jwt);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(applicationService.createApplication(request, actor, httpRequest.getHeader("X-Request-Id")));
    }

    @GetMapping
    @RequiresScope("flags:read")
    @Operation(summary = "List applications")
    PageResponse<ApplicationResponse> listApplications(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String cursor) {
        return applicationService.listApplications(limit, cursor);
    }

    @GetMapping("/{appId}")
    @RequiresScope("flags:read")
    @Operation(summary = "Get application")
    ApplicationResponse getApplication(@PathVariable String appId) {
        return applicationService.getApplication(appId);
    }

    @PutMapping("/{appId}")
    @RequiresScope("admin")
    @Operation(summary = "Update application")
    ApplicationResponse updateApplication(
            @PathVariable String appId,
            @Valid @RequestBody UpdateApplicationRequest request,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {
        return applicationService.updateApplication(appId, request, actorResolver.resolve(jwt),
                httpRequest.getHeader("X-Request-Id"));
    }

    @PostMapping("/{appId}/api-keys")
    @RequiresScope("admin")
    @Operation(summary = "Create API key")
    ResponseEntity<ApiKeyCreatedResponse> createApiKey(
            @PathVariable String appId,
            @Valid @RequestBody CreateApiKeyRequest request,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(applicationService.createApiKey(appId, request, actorResolver.resolve(jwt),
                        httpRequest.getHeader("X-Request-Id")));
    }

    @GetMapping("/{appId}/api-keys")
    @RequiresScope("admin")
    @Operation(summary = "List API keys")
    List<ApiKeyResponse> listApiKeys(@PathVariable String appId) {
        return applicationService.listApiKeys(appId);
    }

    @DeleteMapping("/{appId}/api-keys/{keyId}")
    @RequiresScope("admin")
    @Operation(summary = "Revoke API key")
    ResponseEntity<Void> revokeApiKey(
            @PathVariable String appId,
            @PathVariable UUID keyId,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {
        applicationService.revokeApiKey(appId, keyId, actorResolver.resolve(jwt), httpRequest.getHeader("X-Request-Id"));
        return ResponseEntity.noContent().build();
    }
}
