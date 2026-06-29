package com.fms.management.service;

import com.fms.common.exception.FmsErrorCode;
import com.fms.common.exception.FmsException;
import com.fms.domain.FeatureFlagEntity;
import com.fms.domain.KillSwitchOverrideEntity;
import com.fms.domain.enums.AuditAction;
import com.fms.domain.enums.KillSwitchScope;
import com.fms.management.dto.KillSwitchRequest;
import com.fms.management.dto.KillSwitchResponse;
import com.fms.management.support.AuditRecorder;
import com.fms.observability.FmsMetrics;
import com.fms.repository.EnvironmentConfigRepository;
import com.fms.repository.EnvironmentRepository;
import com.fms.repository.KillSwitchOverrideRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class KillSwitchService {

    private final KillSwitchOverrideRepository killSwitchOverrideRepository;
    private final EnvironmentRepository environmentRepository;
    private final EnvironmentConfigRepository environmentConfigRepository;
    private final FlagService flagService;
    private final AuditRecorder auditRecorder;
    private final FmsMetrics metrics;

    public KillSwitchService(
            KillSwitchOverrideRepository killSwitchOverrideRepository,
            EnvironmentRepository environmentRepository,
            EnvironmentConfigRepository environmentConfigRepository,
            FlagService flagService,
            AuditRecorder auditRecorder,
            FmsMetrics metrics) {
        this.killSwitchOverrideRepository = killSwitchOverrideRepository;
        this.environmentRepository = environmentRepository;
        this.environmentConfigRepository = environmentConfigRepository;
        this.flagService = flagService;
        this.auditRecorder = auditRecorder;
        this.metrics = metrics;
    }

    @Transactional
    public KillSwitchResponse activate(
            String flagKey, KillSwitchRequest request, String actor, String requestId) {
        validateEnvironment(request.environment());
        KillSwitchScope scope = parseScope(request.scope());
        validateScope(scope, request.regionCode());

        FeatureFlagEntity flag = flagService.findFlag(request.appId(), flagKey);
        KillSwitchOverrideEntity override = new KillSwitchOverrideEntity();
        override.setFlag(flag);
        override.setEnvironment(request.environment());
        override.setScope(scope);
        override.setRegionCode(scope == KillSwitchScope.region ? request.regionCode() : null);
        override.setForcedValue(request.forcedValue());
        override.setActivatedBy(actor);
        KillSwitchOverrideEntity saved = killSwitchOverrideRepository.save(override);

        long configVersion = environmentConfigRepository.findById(request.environment())
                .map(cfg -> cfg.getCurrentConfigVersion())
                .orElse(0L);

        auditRecorder.record(actor, AuditAction.kill_switch_on, "feature_flag", flagKey, request.environment(), requestId,
                Map.of("scope", request.scope(), "comment", request.comment() == null ? "" : request.comment()));
        metrics.recordKillSwitchActivation(request.environment(), request.scope());

        return toResponse(flagKey, saved, configVersion);
    }

    @Transactional
    public KillSwitchResponse deactivate(String flagKey, KillSwitchRequest request, String actor, String requestId) {
        FeatureFlagEntity flag = flagService.findFlag(request.appId(), flagKey);
        KillSwitchScope scope = parseScope(request.scope());
        List<KillSwitchOverrideEntity> active = killSwitchOverrideRepository
                .findByFlag_IdAndEnvironmentAndActiveTrue(flag.getId(), request.environment());

        KillSwitchOverrideEntity target = active.stream()
                .filter(o -> o.getScope() == scope)
                .filter(o -> scope == KillSwitchScope.global
                        || request.regionCode() != null && request.regionCode().equals(o.getRegionCode()))
                .findFirst()
                .orElseThrow(() -> new FmsException(FmsErrorCode.VALIDATION_ERROR, "No active kill switch found."));

        target.setActive(false);
        target.setDeactivatedAt(Instant.now());
        target.setDeactivatedBy(actor);
        killSwitchOverrideRepository.save(target);

        long configVersion = environmentConfigRepository.findById(request.environment())
                .map(cfg -> cfg.getCurrentConfigVersion())
                .orElse(0L);

        auditRecorder.record(actor, AuditAction.kill_switch_off, "feature_flag", flagKey, request.environment(), requestId,
                Map.of());

        return toResponse(flagKey, target, configVersion);
    }

    @Transactional(readOnly = true)
    public KillSwitchResponse.KillSwitchListResponse listActive(String appId, String flagKey, String environment) {
        FeatureFlagEntity flag = flagService.findFlag(appId, flagKey);
        List<KillSwitchResponse> overrides = killSwitchOverrideRepository
                .findByFlag_IdAndEnvironmentAndActiveTrue(flag.getId(), environment)
                .stream()
                .map(o -> toResponse(flagKey, o, null))
                .toList();
        return new KillSwitchResponse.KillSwitchListResponse(overrides);
    }

    private KillSwitchResponse toResponse(String flagKey, KillSwitchOverrideEntity entity, Long configVersion) {
        return new KillSwitchResponse(
                flagKey,
                entity.getEnvironment(),
                entity.getScope().name(),
                entity.getRegionCode(),
                entity.isActive(),
                entity.getForcedValue(),
                entity.getActivatedAt(),
                entity.getActivatedBy(),
                configVersion);
    }

    private KillSwitchScope parseScope(String scope) {
        try {
            return KillSwitchScope.valueOf(scope);
        } catch (IllegalArgumentException e) {
            throw new FmsException(FmsErrorCode.VALIDATION_ERROR, "Invalid kill switch scope.");
        }
    }

    private void validateScope(KillSwitchScope scope, String regionCode) {
        if (scope == KillSwitchScope.region && (regionCode == null || regionCode.isBlank())) {
            throw new FmsException(FmsErrorCode.VALIDATION_ERROR, "regionCode is required for region scope.");
        }
    }

    private void validateEnvironment(String environment) {
        if (!environmentRepository.existsById(environment)) {
            throw new FmsException(FmsErrorCode.INVALID_ENVIRONMENT, "Environment not found: " + environment);
        }
    }
}
