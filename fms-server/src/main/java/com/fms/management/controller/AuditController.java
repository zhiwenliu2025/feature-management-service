package com.fms.management.controller;

import com.fms.common.api.PageResponse;
import com.fms.management.service.AuditQueryService;
import com.fms.domain.enums.AuditAction;
import com.fms.management.dto.AuditEventResponse;
import com.fms.management.security.RequiresScope;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/v1/management/audit")
@Tag(name = "Management — Audit", description = "Management-plane audit log")
public class AuditController {

    private final AuditQueryService auditQueryService;

    public AuditController(AuditQueryService auditQueryService) {
        this.auditQueryService = auditQueryService;
    }

    @GetMapping
    @RequiresScope("audit:read")
    @Operation(summary = "Query audit events")
    PageResponse<AuditEventResponse> queryAudit(
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) String environment,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String cursor) {
        return auditQueryService.query(resourceType, resourceId, actor, action, environment, from, to, limit);
    }
}
