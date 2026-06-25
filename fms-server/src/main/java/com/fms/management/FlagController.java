package com.fms.management;

import com.fms.common.api.PageResponse;
import com.fms.domain.enums.FlagStatus;
import com.fms.management.dto.CreateFlagRequest;
import com.fms.management.dto.FlagResponse;
import com.fms.management.dto.PublishFlagRequest;
import com.fms.management.dto.PublishFlagResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/management/flags")
@Tag(name = "Management — Feature Flags", description = "Feature flag lifecycle management")
public class FlagController {

    private final FlagService flagService;

    public FlagController(FlagService flagService) {
        this.flagService = flagService;
    }

    @PostMapping
    @Operation(summary = "Create feature flag")
    ResponseEntity<FlagResponse> createFlag(
            @Valid @RequestBody CreateFlagRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String actor = resolveActor(jwt);
        return ResponseEntity.status(HttpStatus.CREATED).body(flagService.createFlag(request, actor));
    }

    @GetMapping
    @Operation(summary = "List feature flags")
    PageResponse<FlagResponse> listFlags(
            @RequestParam String appId,
            @RequestParam(required = false) FlagStatus status,
            @RequestParam(defaultValue = "20") int limit) {
        return flagService.listFlags(appId, status, limit);
    }

    @GetMapping("/{flagKey}")
    @Operation(summary = "Get feature flag")
    FlagResponse getFlag(@PathVariable String flagKey, @RequestParam String appId) {
        return flagService.getFlag(appId, flagKey);
    }

    @PostMapping("/{flagKey}/publish")
    @Operation(summary = "Publish feature flag to environment")
    ResponseEntity<PublishFlagResponse> publishFlag(
            @PathVariable String flagKey,
            @RequestParam String appId,
            @Valid @RequestBody PublishFlagRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String actor = resolveActor(jwt);
        return ResponseEntity.accepted().body(flagService.publishFlag(appId, flagKey, request, actor));
    }

    private String resolveActor(Jwt jwt) {
        if (jwt == null) {
            return "local-dev";
        }
        String email = jwt.getClaimAsString("email");
        return email != null ? email : jwt.getSubject();
    }
}
