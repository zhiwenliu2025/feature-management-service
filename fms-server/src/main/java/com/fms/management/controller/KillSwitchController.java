package com.fms.management.controller;

import com.fms.management.dto.KillSwitchRequest;
import com.fms.management.service.KillSwitchService;
import com.fms.management.dto.KillSwitchResponse;
import com.fms.management.security.RequiresScope;
import com.fms.management.support.ManagementActorResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/management/flags")
@Tag(name = "Management — Kill Switch", description = "Emergency flag overrides")
public class KillSwitchController {

    private final KillSwitchService killSwitchService;
    private final ManagementActorResolver actorResolver;

    public KillSwitchController(KillSwitchService killSwitchService, ManagementActorResolver actorResolver) {
        this.killSwitchService = killSwitchService;
        this.actorResolver = actorResolver;
    }

    @PostMapping("/{flagKey}/kill-switch")
    @RequiresScope("flags:kill")
    @Operation(summary = "Activate kill switch")
    KillSwitchResponse activate(
            @PathVariable String flagKey,
            @Valid @RequestBody KillSwitchRequest request,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {
        return killSwitchService.activate(flagKey, request, actorResolver.resolve(jwt),
                httpRequest.getHeader("X-Request-Id"));
    }

    @DeleteMapping("/{flagKey}/kill-switch")
    @RequiresScope("flags:kill")
    @Operation(summary = "Deactivate kill switch")
    KillSwitchResponse deactivate(
            @PathVariable String flagKey,
            @Valid @RequestBody KillSwitchRequest request,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {
        return killSwitchService.deactivate(flagKey, request, actorResolver.resolve(jwt),
                httpRequest.getHeader("X-Request-Id"));
    }

    @GetMapping("/{flagKey}/kill-switch")
    @RequiresScope("flags:read")
    @Operation(summary = "List active kill switch overrides")
    KillSwitchResponse.KillSwitchListResponse listActive(
            @PathVariable String flagKey,
            @RequestParam String appId,
            @RequestParam String environment) {
        return killSwitchService.listActive(appId, flagKey, environment);
    }
}
