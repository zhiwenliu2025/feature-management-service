package com.fms.management.controller;

import com.fms.management.dto.EnvironmentConfigResponse;
import com.fms.management.service.EnvironmentManagementService;
import com.fms.management.dto.EnvironmentResponse;
import com.fms.management.dto.PromoteRequest;
import com.fms.management.dto.PromoteResponse;
import com.fms.management.security.RequiresScope;
import com.fms.management.support.ManagementActorResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/management/environments")
@Tag(name = "Management — Environments", description = "Environment configuration and promotion")
public class EnvironmentController {

    private final EnvironmentManagementService environmentService;
    private final ManagementActorResolver actorResolver;

    public EnvironmentController(
            EnvironmentManagementService environmentService,
            ManagementActorResolver actorResolver) {
        this.environmentService = environmentService;
        this.actorResolver = actorResolver;
    }

    @GetMapping
    @RequiresScope("flags:read")
    @Operation(summary = "List environments")
    List<EnvironmentResponse> listEnvironments() {
        return environmentService.listEnvironments();
    }

    @GetMapping("/{env}/config")
    @RequiresScope("flags:read")
    @Operation(summary = "Get environment config version")
    EnvironmentConfigResponse getConfig(@PathVariable("env") String environment) {
        return environmentService.getEnvironmentConfig(environment);
    }

    @PostMapping("/{env}/promote")
    @RequiresScope("flags:publish")
    @Operation(summary = "Promote configuration from source environment")
    ResponseEntity<PromoteResponse> promote(
            @PathVariable("env") String targetEnvironment,
            @Valid @RequestBody PromoteRequest request,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {
        return ResponseEntity.accepted()
                .body(environmentService.promote(targetEnvironment, request, actorResolver.resolve(jwt),
                        httpRequest.getHeader("X-Request-Id")));
    }
}
