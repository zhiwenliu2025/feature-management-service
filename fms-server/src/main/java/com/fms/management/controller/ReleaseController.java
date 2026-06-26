package com.fms.management.controller;

import com.fms.common.api.PageResponse;
import com.fms.management.dto.CreateReleaseRequest;
import com.fms.management.dto.LinkFlagsRequest;
import com.fms.management.dto.ReleaseDetailResponse;
import com.fms.management.dto.ReleaseResponse;
import com.fms.management.security.RequiresScope;
import com.fms.management.service.ReleaseService;
import com.fms.management.support.ManagementActorResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
@RequestMapping("/v1/management/releases")
@Tag(name = "Management — Releases", description = "Release records and flag linkage")
public class ReleaseController {

    private final ReleaseService releaseService;
    private final ManagementActorResolver actorResolver;

    public ReleaseController(ReleaseService releaseService, ManagementActorResolver actorResolver) {
        this.releaseService = releaseService;
        this.actorResolver = actorResolver;
    }

    @PostMapping
    @RequiresScope("flags:write")
    @Operation(summary = "Create release record")
    ResponseEntity<ReleaseResponse> createRelease(
            @Valid @RequestBody CreateReleaseRequest request,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(releaseService.createRelease(request, actorResolver.resolve(jwt),
                        httpRequest.getHeader("X-Request-Id")));
    }

    @GetMapping
    @RequiresScope("flags:read")
    @Operation(summary = "List releases")
    PageResponse<ReleaseResponse> listReleases(@RequestParam(defaultValue = "20") int limit) {
        return releaseService.listReleases(limit);
    }

    @GetMapping("/{releaseId}")
    @RequiresScope("flags:read")
    @Operation(summary = "Get release with linked flags")
    ReleaseDetailResponse getRelease(@PathVariable String releaseId) {
        return releaseService.getRelease(releaseId);
    }

    @PostMapping("/{releaseId}/flags")
    @RequiresScope("flags:write")
    @Operation(summary = "Link flags to release")
    ReleaseDetailResponse linkFlags(
            @PathVariable String releaseId,
            @Valid @RequestBody LinkFlagsRequest request,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {
        return releaseService.linkFlags(releaseId, request, actorResolver.resolve(jwt),
                httpRequest.getHeader("X-Request-Id"));
    }
}
