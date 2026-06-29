package com.fms.management.service;

import com.fms.common.exception.FmsErrorCode;
import com.fms.common.exception.FmsException;
import com.fms.domain.EnvironmentConfigEntity;
import com.fms.domain.EnvironmentEntity;
import com.fms.domain.FeatureFlagEntity;
import com.fms.domain.FlagVersionEntity;
import com.fms.domain.enums.AuditAction;
import com.fms.management.dto.EnvironmentConfigResponse;
import com.fms.management.dto.EnvironmentResponse;
import com.fms.management.dto.PromoteRequest;
import com.fms.management.dto.PromoteResponse;
import com.fms.management.support.AuditRecorder;
import com.fms.repository.EnvironmentConfigRepository;
import com.fms.repository.EnvironmentRepository;
import com.fms.repository.FlagVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class EnvironmentManagementService {

    private final EnvironmentRepository environmentRepository;
    private final EnvironmentConfigRepository environmentConfigRepository;
    private final FlagVersionRepository flagVersionRepository;
    private final FlagService flagService;
    private final PublishOrchestrator publishOrchestrator;
    private final AuditRecorder auditRecorder;

    public EnvironmentManagementService(
            EnvironmentRepository environmentRepository,
            EnvironmentConfigRepository environmentConfigRepository,
            FlagVersionRepository flagVersionRepository,
            FlagService flagService,
            PublishOrchestrator publishOrchestrator,
            AuditRecorder auditRecorder) {
        this.environmentRepository = environmentRepository;
        this.environmentConfigRepository = environmentConfigRepository;
        this.flagVersionRepository = flagVersionRepository;
        this.flagService = flagService;
        this.publishOrchestrator = publishOrchestrator;
        this.auditRecorder = auditRecorder;
    }

    @Transactional(readOnly = true)
    public List<EnvironmentResponse> listEnvironments() {
        return environmentRepository.findAll().stream()
                .sorted(Comparator.comparingInt(EnvironmentEntity::getSortOrder))
                .map(env -> new EnvironmentResponse(
                        env.getName(),
                        env.getDisplayName(),
                        env.getSortOrder(),
                        env.isProduction()))
                .toList();
    }

    @Transactional(readOnly = true)
    public EnvironmentConfigResponse getEnvironmentConfig(String environment) {
        EnvironmentConfigEntity config = environmentConfigRepository.findById(environment)
                .orElseThrow(() -> new FmsException(FmsErrorCode.INVALID_ENVIRONMENT,
                        "Environment not found: " + environment));
        return new EnvironmentConfigResponse(
                config.getEnvironment(),
                config.getCurrentConfigVersion(),
                config.getUpdatedAt());
    }

    @Transactional
    public PromoteResponse promote(String targetEnvironment, PromoteRequest request, String actor, String requestId) {
        validatePromotionOrder(request.sourceEnvironment(), targetEnvironment);

        List<String> jobIds = new ArrayList<>();
        long lastConfigVersion = environmentConfigRepository.findById(targetEnvironment)
                .map(EnvironmentConfigEntity::getCurrentConfigVersion)
                .orElse(0L);

        for (String flagKey : request.flagKeys()) {
            FeatureFlagEntity flag = flagService.findFlag(request.appId(), flagKey);
            FlagVersionEntity sourceVersion = flagVersionRepository
                    .findByFlag_IdAndEnvironmentOrderByFlagVersionDesc(flag.getId(), request.sourceEnvironment(),
                            org.springframework.data.domain.PageRequest.of(0, 1))
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new FmsException(FmsErrorCode.VERSION_NOT_FOUND,
                            "No published version in source environment for flag: " + flagKey));

            var response = publishOrchestrator.promoteFromSnapshot(
                    flag,
                    request.appId(),
                    targetEnvironment,
                    sourceVersion.getSnapshot(),
                    request.releaseId(),
                    request.comment(),
                    actor,
                    requestId);
            jobIds.add(response.publishJobId());
            lastConfigVersion = response.configVersion();
        }

        auditRecorder.record(actor, AuditAction.promote, "environment", targetEnvironment, targetEnvironment, requestId,
                Map.of("sourceEnvironment", request.sourceEnvironment(), "flagKeys", request.flagKeys()));

        return new PromoteResponse(
                targetEnvironment,
                request.sourceEnvironment(),
                request.flagKeys(),
                lastConfigVersion,
                jobIds);
    }

    private void validatePromotionOrder(String source, String target) {
        EnvironmentEntity sourceEnv = environmentRepository.findById(source)
                .orElseThrow(() -> new FmsException(FmsErrorCode.INVALID_ENVIRONMENT, "Source environment not found."));
        EnvironmentEntity targetEnv = environmentRepository.findById(target)
                .orElseThrow(() -> new FmsException(FmsErrorCode.INVALID_ENVIRONMENT, "Target environment not found."));
        if (sourceEnv.getSortOrder() >= targetEnv.getSortOrder()) {
            throw new FmsException(FmsErrorCode.VALIDATION_ERROR,
                    "Promotion must move from lower to higher environment.");
        }
    }
}
