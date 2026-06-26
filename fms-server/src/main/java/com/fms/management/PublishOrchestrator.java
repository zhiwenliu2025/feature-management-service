package com.fms.management;

import com.fms.common.exception.FmsErrorCode;
import com.fms.common.exception.FmsException;
import com.fms.domain.ConfigVersionHistoryEntity;
import com.fms.domain.EnvironmentConfigEntity;
import com.fms.domain.FeatureFlagEntity;
import com.fms.domain.FlagEnvironmentStateEntity;
import com.fms.domain.FlagRuleEntity;
import com.fms.domain.FlagVersionEntity;
import com.fms.domain.PublishJobEntity;
import com.fms.domain.ReleaseEntity;
import com.fms.domain.enums.AuditAction;
import com.fms.domain.enums.FlagStatus;
import com.fms.domain.enums.PublishJobStatus;
import com.fms.domain.enums.PublishJobType;
import com.fms.management.dto.PublishFlagRequest;
import com.fms.management.dto.PublishFlagResponse;
import com.fms.management.dto.RollbackFlagRequest;
import com.fms.management.support.AuditRecorder;
import com.fms.management.support.SnapshotBuilder;
import com.fms.repository.ConfigVersionHistoryRepository;
import com.fms.repository.EnvironmentConfigRepository;
import com.fms.repository.EnvironmentRepository;
import com.fms.repository.FlagEnvironmentStateRepository;
import com.fms.repository.FlagRuleRepository;
import com.fms.repository.FlagVersionRepository;
import com.fms.repository.PublishJobRepository;
import com.fms.repository.ReleaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PublishOrchestrator {

    private final EnvironmentRepository environmentRepository;
    private final EnvironmentConfigRepository environmentConfigRepository;
    private final FlagRuleRepository flagRuleRepository;
    private final FlagVersionRepository flagVersionRepository;
    private final FlagEnvironmentStateRepository flagEnvironmentStateRepository;
    private final PublishJobRepository publishJobRepository;
    private final ConfigVersionHistoryRepository configVersionHistoryRepository;
    private final ReleaseRepository releaseRepository;
    private final AuditRecorder auditRecorder;

    public PublishOrchestrator(
            EnvironmentRepository environmentRepository,
            EnvironmentConfigRepository environmentConfigRepository,
            FlagRuleRepository flagRuleRepository,
            FlagVersionRepository flagVersionRepository,
            FlagEnvironmentStateRepository flagEnvironmentStateRepository,
            PublishJobRepository publishJobRepository,
            ConfigVersionHistoryRepository configVersionHistoryRepository,
            ReleaseRepository releaseRepository,
            AuditRecorder auditRecorder) {
        this.environmentRepository = environmentRepository;
        this.environmentConfigRepository = environmentConfigRepository;
        this.flagRuleRepository = flagRuleRepository;
        this.flagVersionRepository = flagVersionRepository;
        this.flagEnvironmentStateRepository = flagEnvironmentStateRepository;
        this.publishJobRepository = publishJobRepository;
        this.configVersionHistoryRepository = configVersionHistoryRepository;
        this.releaseRepository = releaseRepository;
        this.auditRecorder = auditRecorder;
    }

    @Transactional
    public PublishFlagResponse promoteFromSnapshot(
            FeatureFlagEntity flag,
            String appId,
            String targetEnvironment,
            Object snapshot,
            String releaseId,
            String comment,
            String actor,
            String requestId) {
        validateEnvironment(targetEnvironment);
        ReleaseEntity release = null;
        if (releaseId != null && !releaseId.isBlank()) {
            release = releaseRepository.findByReleaseId(releaseId)
                    .orElseThrow(() -> new FmsException(FmsErrorCode.RELEASE_NOT_FOUND,
                            "Release not found: " + releaseId));
        }

        long configVersion = allocateConfigVersion(targetEnvironment);
        int flagVersion = flagVersionRepository.countByFlag_IdAndEnvironment(flag.getId(), targetEnvironment) + 1;

        FlagVersionEntity versionEntity = new FlagVersionEntity();
        versionEntity.setFlag(flag);
        versionEntity.setEnvironment(targetEnvironment);
        versionEntity.setConfigVersion(configVersion);
        versionEntity.setFlagVersion(flagVersion);
        versionEntity.setSnapshot(snapshot);
        versionEntity.setRelease(release);
        versionEntity.setComment(comment);
        versionEntity.setKillSwitch(false);
        versionEntity.setPublishedBy(actor);
        versionEntity.setPublishedAt(Instant.now());
        FlagVersionEntity savedVersion = flagVersionRepository.save(versionEntity);

        upsertEnvironmentState(flag, targetEnvironment, savedVersion.getId());
        flag.setUpdatedBy(actor);

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = snapshot instanceof Map<?, ?> map
                ? new HashMap<>((Map<String, Object>) map)
                : Map.of();
        PublishJobEntity job = createJob(flag, targetEnvironment, configVersion, savedVersion, PublishJobType.promote, payload);
        PublishJobEntity savedJob = publishJobRepository.save(job);
        saveConfigHistory(targetEnvironment, configVersion, flag.getId(), savedJob.getId());

        auditRecorder.record(actor, AuditAction.promote, "feature_flag", flag.getKey(), targetEnvironment, requestId,
                Map.of("after", Map.of("configVersion", configVersion, "flagVersion", flagVersion)));

        return new PublishFlagResponse(
                flag.getKey(),
                appId,
                targetEnvironment,
                configVersion,
                flagVersion,
                String.valueOf(savedJob.getId()),
                PublishJobStatus.pending.name(),
                savedVersion.getPublishedAt(),
                actor);
    }

    @Transactional
    public PublishFlagResponse publish(
            FeatureFlagEntity flag,
            String appId,
            PublishFlagRequest request,
            String actor,
            String requestId) {
        validateEnvironment(request.environment());
        List<FlagRuleEntity> rules = flagRuleRepository.findByFlag_IdAndEnvironmentOrderByPriorityAsc(
                flag.getId(), request.environment());

        ReleaseEntity release = null;
        String releaseExternalId = null;
        if (request.releaseId() != null && !request.releaseId().isBlank()) {
            release = releaseRepository.findByReleaseId(request.releaseId())
                    .orElseThrow(() -> new FmsException(FmsErrorCode.RELEASE_NOT_FOUND,
                            "Release not found: " + request.releaseId()));
            releaseExternalId = release.getReleaseId();
        }

        long configVersion = allocateConfigVersion(request.environment());
        int flagVersion = flagVersionRepository.countByFlag_IdAndEnvironment(flag.getId(), request.environment()) + 1;

        Map<String, Object> snapshot = SnapshotBuilder.build(flag, rules, releaseExternalId);
        FlagVersionEntity versionEntity = new FlagVersionEntity();
        versionEntity.setFlag(flag);
        versionEntity.setEnvironment(request.environment());
        versionEntity.setConfigVersion(configVersion);
        versionEntity.setFlagVersion(flagVersion);
        versionEntity.setSnapshot(snapshot);
        versionEntity.setRelease(release);
        versionEntity.setComment(request.comment());
        versionEntity.setKillSwitch(request.killSwitch());
        versionEntity.setPublishedBy(actor);
        versionEntity.setPublishedAt(Instant.now());
        FlagVersionEntity savedVersion = flagVersionRepository.save(versionEntity);

        upsertEnvironmentState(flag, request.environment(), savedVersion.getId());

        if (flag.getStatus() == FlagStatus.draft) {
            flag.setStatus(FlagStatus.published);
        }
        flag.setUpdatedBy(actor);

        PublishJobEntity job = createJob(flag, request.environment(), configVersion, savedVersion, PublishJobType.publish, snapshot);
        PublishJobEntity savedJob = publishJobRepository.save(job);

        saveConfigHistory(request.environment(), configVersion, flag.getId(), savedJob.getId());

        long beforeVersion = configVersion - 1;
        auditRecorder.record(actor, AuditAction.publish, "feature_flag", flag.getKey(), request.environment(), requestId,
                Map.of("before", Map.of("configVersion", beforeVersion),
                        "after", Map.of("configVersion", configVersion, "flagVersion", flagVersion),
                        "changedFields", List.of("rules", "configVersion")));

        return new PublishFlagResponse(
                flag.getKey(),
                appId,
                request.environment(),
                configVersion,
                flagVersion,
                String.valueOf(savedJob.getId()),
                PublishJobStatus.pending.name(),
                savedVersion.getPublishedAt(),
                actor);
    }

    @Transactional
    public PublishFlagResponse rollback(
            FeatureFlagEntity flag,
            RollbackFlagRequest request,
            String actor,
            String requestId) {
        validateEnvironment(request.environment());
        FlagVersionEntity target = flagVersionRepository
                .findByFlag_IdAndEnvironmentAndFlagVersion(flag.getId(), request.environment(), request.targetFlagVersion())
                .orElseThrow(() -> new FmsException(FmsErrorCode.ROLLBACK_TARGET_NOT_FOUND,
                        "Target flag version not found."));

        long configVersion = allocateConfigVersion(request.environment());
        int flagVersion = flagVersionRepository.countByFlag_IdAndEnvironment(flag.getId(), request.environment()) + 1;

        FlagVersionEntity versionEntity = new FlagVersionEntity();
        versionEntity.setFlag(flag);
        versionEntity.setEnvironment(request.environment());
        versionEntity.setConfigVersion(configVersion);
        versionEntity.setFlagVersion(flagVersion);
        versionEntity.setSnapshot(target.getSnapshot());
        versionEntity.setRelease(target.getRelease());
        versionEntity.setComment(request.comment());
        versionEntity.setKillSwitch(false);
        versionEntity.setPublishedBy(actor);
        versionEntity.setPublishedAt(Instant.now());
        FlagVersionEntity savedVersion = flagVersionRepository.save(versionEntity);

        upsertEnvironmentState(flag, request.environment(), savedVersion.getId());
        flag.setUpdatedBy(actor);

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = target.getSnapshot() instanceof Map<?, ?> map
                ? new HashMap<>((Map<String, Object>) map)
                : Map.of();
        PublishJobEntity job = createJob(flag, request.environment(), configVersion, savedVersion, PublishJobType.rollback, payload);
        PublishJobEntity savedJob = publishJobRepository.save(job);
        saveConfigHistory(request.environment(), configVersion, flag.getId(), savedJob.getId());

        auditRecorder.record(actor, AuditAction.rollback, "feature_flag", flag.getKey(), request.environment(), requestId,
                Map.of("before", Map.of("flagVersion", request.targetFlagVersion()),
                        "after", Map.of("configVersion", configVersion, "flagVersion", flagVersion)));

        return new PublishFlagResponse(
                flag.getKey(),
                request.appId(),
                request.environment(),
                configVersion,
                flagVersion,
                String.valueOf(savedJob.getId()),
                PublishJobStatus.pending.name(),
                savedVersion.getPublishedAt(),
                actor);
    }

    private long allocateConfigVersion(String environment) {
        EnvironmentConfigEntity config = environmentConfigRepository.findByEnvironmentForUpdate(environment)
                .orElseThrow(() -> new FmsException(FmsErrorCode.INVALID_ENVIRONMENT,
                        "Environment not found: " + environment));
        long next = config.getCurrentConfigVersion() + 1;
        config.setCurrentConfigVersion(next);
        config.setUpdatedAt(Instant.now());
        environmentConfigRepository.save(config);
        return next;
    }

    private void upsertEnvironmentState(FeatureFlagEntity flag, String environment, Long versionId) {
        FlagEnvironmentStateEntity state = flagEnvironmentStateRepository
                .findByFlag_IdAndId_Environment(flag.getId(), environment)
                .orElseGet(() -> {
                    FlagEnvironmentStateEntity created = new FlagEnvironmentStateEntity();
                    created.setFlag(flag);
                    created.setEnvironment(environment);
                    return created;
                });
        state.setPublished(true);
        state.setLatestVersionId(versionId);
        state.setDraftDirty(false);
        flagEnvironmentStateRepository.save(state);
    }

    private PublishJobEntity createJob(
            FeatureFlagEntity flag,
            String environment,
            long configVersion,
            FlagVersionEntity version,
            PublishJobType jobType,
            Map<String, Object> payload) {
        PublishJobEntity job = new PublishJobEntity();
        job.setFlag(flag);
        job.setFlagKey(flag.getKey());
        job.setEnvironment(environment);
        job.setConfigVersion(configVersion);
        job.setFlagVersionId(version.getId());
        job.setJobType(jobType);
        job.setPayload(payload);
        job.setStatus(PublishJobStatus.pending);
        return job;
    }

    private void saveConfigHistory(String environment, long configVersion, UUID flagId, Long jobId) {
        ConfigVersionHistoryEntity history = new ConfigVersionHistoryEntity();
        history.setEnvironment(environment);
        history.setConfigVersion(configVersion);
        history.setChangedFlagIds(new UUID[]{flagId});
        history.setDeletedFlagKeys(new String[0]);
        history.setPublishJobId(jobId);
        configVersionHistoryRepository.save(history);
    }

    private void validateEnvironment(String environment) {
        if (!environmentRepository.existsById(environment)) {
            throw new FmsException(FmsErrorCode.INVALID_ENVIRONMENT, "Environment not found: " + environment);
        }
    }
}
