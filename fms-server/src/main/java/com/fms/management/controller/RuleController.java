package com.fms.management;

import com.fms.management.dto.FlagDetailResponse;
import com.fms.management.dto.ReplaceRulesRequest;
import com.fms.management.dto.UpdateRuleRequest;
import com.fms.management.security.RequiresScope;
import com.fms.management.support.ManagementActorResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/management/flags")
@Tag(name = "Management — Rules", description = "Feature flag targeting rules")
public class RuleController {

    private final RuleService ruleService;
    private final ManagementActorResolver actorResolver;

    public RuleController(RuleService ruleService, ManagementActorResolver actorResolver) {
        this.ruleService = ruleService;
        this.actorResolver = actorResolver;
    }

    @PutMapping("/{flagKey}/rules")
    @RequiresScope("flags:write")
    @Operation(summary = "Replace all rules for an environment")
    FlagDetailResponse replaceRules(
            @PathVariable String flagKey,
            @RequestParam String appId,
            @Valid @RequestBody ReplaceRulesRequest request,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {
        return ruleService.replaceRules(appId, flagKey, request, actorResolver.resolve(jwt),
                httpRequest.getHeader("X-Request-Id"));
    }

    @PatchMapping("/{flagKey}/rules/{ruleId}")
    @RequiresScope("flags:write")
    @Operation(summary = "Update a single rule")
    FlagDetailResponse updateRule(
            @PathVariable String flagKey,
            @PathVariable UUID ruleId,
            @RequestParam String appId,
            @RequestParam String environment,
            @Valid @RequestBody UpdateRuleRequest request,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {
        return ruleService.updateRule(appId, flagKey, ruleId, environment, request, actorResolver.resolve(jwt),
                httpRequest.getHeader("X-Request-Id"));
    }
}
