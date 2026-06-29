package com.fms.management.controller;

import com.fms.common.api.PageResponse;
import com.fms.management.service.FlagService;
import com.fms.domain.enums.FlagStatus;
import com.fms.management.dto.CreateFlagRequest;
import com.fms.management.dto.FlagDetailResponse;
import com.fms.management.dto.FlagSummaryResponse;
import com.fms.management.dto.FlagVersionDetailResponse;
import com.fms.management.dto.FlagVersionSummaryResponse;
import com.fms.management.dto.PublishFlagRequest;
import com.fms.management.dto.PublishFlagResponse;
import com.fms.management.dto.RollbackFlagRequest;
import com.fms.management.dto.UpdateFlagRequest;
import com.fms.management.idempotency.Idempotent;
import com.fms.management.security.RequiresScope;
import com.fms.management.support.ManagementActorResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
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

@RestController
@RequestMapping("/v1/management/flags")
@Tag(name = "Management — Feature Flags", description = "Feature flag lifecycle management")
public class FlagController {

    private final FlagService flagService;
    private final ManagementActorResolver actorResolver;

    public FlagController(FlagService flagService, ManagementActorResolver actorResolver) {
        this.flagService = flagService;
        this.actorResolver = actorResolver;
    }

    @PostMapping
    @RequiresScope("flags:write")
    @Operation(summary = "Create feature flag")
    ResponseEntity<FlagDetailResponse> createFlag(
            @Valid @RequestBody CreateFlagRequest request,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(flagService.createFlag(request, actorResolver.resolve(jwt), httpRequest.getHeader("X-Request-Id")));
    }

    @GetMapping
    @RequiresScope("flags:read")
    @Operation(summary = "List feature flags")
    PageResponse<FlagSummaryResponse> listFlags(
            @RequestParam String appId,
            @RequestParam(required = false) FlagStatus status,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String cursor) {
        return flagService.listFlags(appId, tag, status, search, limit, cursor);
    }

    @GetMapping("/{flagKey}")
    @RequiresScope("flags:read")
    @Operation(summary = "Get feature flag with rules")
    FlagDetailResponse getFlag(@PathVariable String flagKey, @RequestParam String appId) {
        return flagService.getFlag(appId, flagKey);
    }

    @PutMapping("/{flagKey}")
    @RequiresScope("flags:write")
    @Operation(summary = "Update feature flag metadata")
    FlagDetailResponse updateFlag(
            @PathVariable String flagKey,
            @RequestParam String appId,
            @Valid @RequestBody UpdateFlagRequest request,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {
        return flagService.updateFlag(appId, flagKey, request, actorResolver.resolve(jwt),
                httpRequest.getHeader("X-Request-Id"));
    }

    @DeleteMapping("/{flagKey}")
    @RequiresScope("flags:write")
    @Operation(summary = "Archive feature flag")
    ResponseEntity<Void> archiveFlag(
            @PathVariable String flagKey,
            @RequestParam String appId,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {
        flagService.archiveFlag(appId, flagKey, actorResolver.resolve(jwt), httpRequest.getHeader("X-Request-Id"));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{flagKey}/versions")
    @RequiresScope("flags:read")
    @Operation(summary = "List flag version history")
    PageResponse<FlagVersionSummaryResponse> listVersions(
            @PathVariable String flagKey,
            @RequestParam String appId,
            @RequestParam String environment,
            @RequestParam(defaultValue = "20") int limit) {
        return flagService.listVersions(appId, flagKey, environment, limit);
    }

    @GetMapping("/{flagKey}/versions/{version}")
    @RequiresScope("flags:read")
    @Operation(summary = "Get published flag snapshot")
    FlagVersionDetailResponse getVersion(
            @PathVariable String flagKey,
            @PathVariable int version,
            @RequestParam String appId,
            @RequestParam String environment) {
        return flagService.getVersion(appId, flagKey, version, environment);
    }

    @PostMapping("/{flagKey}/publish")
    @RequiresScope("flags:publish")
    @Idempotent
    @Operation(summary = "Publish feature flag to environment")
    ResponseEntity<PublishFlagResponse> publishFlag(
            @PathVariable String flagKey,
            @RequestParam String appId,
            @Valid @RequestBody PublishFlagRequest request,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {
        PublishFlagResponse response = flagService.publishFlag(
                appId, flagKey, request, actorResolver.resolve(jwt), httpRequest.getHeader("X-Request-Id"));
        return ResponseEntity.accepted()
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .header("X-Config-Version", String.valueOf(response.configVersion()))
                .body(response);
    }

    @PostMapping("/{flagKey}/rollback")
    @RequiresScope("flags:publish")
    @Idempotent
    @Operation(summary = "Rollback feature flag to prior version")
    ResponseEntity<PublishFlagResponse> rollbackFlag(
            @PathVariable String flagKey,
            @Valid @RequestBody RollbackFlagRequest request,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {
        PublishFlagResponse response = flagService.rollbackFlag(
                flagKey, request, actorResolver.resolve(jwt), httpRequest.getHeader("X-Request-Id"));
        return ResponseEntity.accepted()
                .header("X-Config-Version", String.valueOf(response.configVersion()))
                .body(response);
    }
}
