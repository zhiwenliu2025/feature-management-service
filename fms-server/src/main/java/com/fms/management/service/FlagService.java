package com.fms.management.service;

import com.fms.common.api.PageResponse;
import com.fms.common.exception.FmsErrorCode;
import com.fms.common.exception.FmsException;
import com.fms.domain.ApplicationEntity;
import com.fms.domain.FeatureFlagEntity;
import com.fms.domain.FlagEnvironmentStateEntity;
import com.fms.domain.FlagRuleEntity;
import com.fms.domain.FlagVersionEntity;
import com.fms.domain.TagEntity;
import com.fms.domain.enums.AuditAction;
import com.fms.domain.enums.FlagStatus;
import com.fms.domain.enums.FlagType;
import com.fms.management.dto.CreateFlagRequest;
import com.fms.management.dto.FlagDetailResponse;
import com.fms.management.dto.FlagSummaryResponse;
import com.fms.management.dto.FlagVersionDetailResponse;
import com.fms.management.dto.FlagVersionSummaryResponse;
import com.fms.management.dto.PublishFlagRequest;
import com.fms.management.dto.PublishFlagResponse;
import com.fms.management.dto.RollbackFlagRequest;
import com.fms.management.dto.UpdateFlagRequest;
import com.fms.management.support.AuditRecorder;
import com.fms.management.support.CursorCodec;
import com.fms.repository.ApplicationRepository;
import com.fms.repository.EnvironmentConfigRepository;
import com.fms.repository.FeatureFlagRepository;
import com.fms.repository.FlagEnvironmentStateRepository;
import com.fms.repository.FlagRuleRepository;
import com.fms.repository.FlagVersionRepository;
import com.fms.repository.TagRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FlagService {

    private static final int MAX_PAGE = 100;

    private final ApplicationRepository applicationRepository;
    private final FeatureFlagRepository featureFlagRepository;
    private final TagRepository tagRepository;
    private final FlagRuleRepository flagRuleRepository;
    private final FlagEnvironmentStateRepository flagEnvironmentStateRepository;
    private final FlagVersionRepository flagVersionRepository;
    private final EnvironmentConfigRepository environmentConfigRepository;
    private final PublishOrchestrator publishOrchestrator;
    private final AuditRecorder auditRecorder;
    private final SecureRandom secureRandom = new SecureRandom();

    public FlagService(
            ApplicationRepository applicationRepository,
            FeatureFlagRepository featureFlagRepository,
            TagRepository tagRepository,
            FlagRuleRepository flagRuleRepository,
            FlagEnvironmentStateRepository flagEnvironmentStateRepository,
            FlagVersionRepository flagVersionRepository,
            EnvironmentConfigRepository environmentConfigRepository,
            PublishOrchestrator publishOrchestrator,
            AuditRecorder auditRecorder) {
        this.applicationRepository = applicationRepository;
        this.featureFlagRepository = featureFlagRepository;
        this.tagRepository = tagRepository;
        this.flagRuleRepository = flagRuleRepository;
        this.flagEnvironmentStateRepository = flagEnvironmentStateRepository;
        this.flagVersionRepository = flagVersionRepository;
        this.environmentConfigRepository = environmentConfigRepository;
        this.publishOrchestrator = publishOrchestrator;
        this.auditRecorder = auditRecorder;
    }

    @Transactional
    public FlagDetailResponse createFlag(CreateFlagRequest request, String actor, String requestId) {
        ApplicationEntity application = applicationRepository.findBySlug(request.appId())
                .orElseThrow(() -> new FmsException(FmsErrorCode.APPLICATION_NOT_FOUND,
                        "Application not found: " + request.appId()));

        if (featureFlagRepository.existsByApplication_SlugAndKey(request.appId(), request.key())) {
            throw new FmsException(FmsErrorCode.FLAG_ALREADY_EXISTS, "Flag already exists for application.");
        }

        validateDefaultValue(request.type(), request.defaultValue());

        FeatureFlagEntity flag = new FeatureFlagEntity();
        flag.setApplication(application);
        flag.setKey(request.key());
        flag.setName(request.name());
        flag.setDescription(request.description());
        flag.setType(FlagType.fromExternal(request.type()));
        flag.setDefaultValue(request.defaultValue());
        flag.setRolloutSalt(generateRolloutSalt());
        flag.setCreatedBy(actor);
        flag.setStatus(FlagStatus.draft);

        FeatureFlagEntity saved = featureFlagRepository.save(flag);
        linkTags(saved, request.tags());

        auditRecorder.record(actor, AuditAction.create, "feature_flag", saved.getKey(), null, requestId,
                Map.of("after", Map.of("key", saved.getKey(), "appId", request.appId())));

        return toDetailResponse(saved);
    }

    @Transactional(readOnly = true)
    public FlagDetailResponse getFlag(String appId, String flagKey) {
        return toDetailResponse(findFlag(appId, flagKey));
    }

    @Transactional(readOnly = true)
    public PageResponse<FlagSummaryResponse> listFlags(
            String appId, String tag, FlagStatus status, String search, int limit, String cursor) {
        int pageSize = Math.min(Math.max(limit, 1), MAX_PAGE);
        CursorCodec.Cursor decoded = CursorCodec.decode(cursor);
        List<FeatureFlagEntity> flags;
        if (decoded == null) {
            flags = featureFlagRepository.searchFlags(
                    appId,
                    status == null ? null : status.name(),
                    blankToNull(search),
                    blankToNull(tag),
                    pageSize + 1);
        } else {
            flags = featureFlagRepository.searchFlagsAfterCursor(
                    appId,
                    status == null ? null : status.name(),
                    blankToNull(search),
                    blankToNull(tag),
                    decoded.createdAt(),
                    decoded.id().toString(),
                    pageSize + 1);
        }

        boolean hasMore = flags.size() > pageSize;
        if (hasMore) {
            flags = flags.subList(0, pageSize);
        }

        List<FlagSummaryResponse> data = flags.stream().map(this::toSummary).toList();
        String nextCursor = hasMore && !flags.isEmpty()
                ? CursorCodec.encode(flags.getLast().getCreatedAt(), flags.getLast().getId())
                : null;
        long total = featureFlagRepository.countSearchFlags(
                appId,
                status == null ? null : status.name(),
                blankToNull(search),
                blankToNull(tag));

        return new PageResponse<>(data, PageResponse.Pagination.of(nextCursor, hasMore, total));
    }

    @Transactional
    public FlagDetailResponse updateFlag(String appId, String flagKey, UpdateFlagRequest request, String actor, String requestId) {
        FeatureFlagEntity flag = findFlag(appId, flagKey);
        if (flag.getStatus() == FlagStatus.archived) {
            throw new FmsException(FmsErrorCode.VALIDATION_ERROR, "Cannot update archived flag.");
        }
        if (request.name() != null) {
            flag.setName(request.name());
        }
        if (request.description() != null) {
            flag.setDescription(request.description());
        }
        flag.setUpdatedBy(actor);
        if (request.tags() != null) {
            linkTags(flag, request.tags());
        }
        FeatureFlagEntity saved = featureFlagRepository.save(flag);
        markAllEnvironmentsDirty(saved);
        auditRecorder.record(actor, AuditAction.update, "feature_flag", flagKey, null, requestId, Map.of());
        return toDetailResponse(saved);
    }

    @Transactional
    public void archiveFlag(String appId, String flagKey, String actor, String requestId) {
        FeatureFlagEntity flag = findFlag(appId, flagKey);
        flag.setStatus(FlagStatus.archived);
        flag.setUpdatedBy(actor);
        featureFlagRepository.save(flag);
        auditRecorder.record(actor, AuditAction.archive, "feature_flag", flagKey, null, requestId, Map.of());
    }

    @Transactional(readOnly = true)
    public PageResponse<FlagVersionSummaryResponse> listVersions(String appId, String flagKey, String environment, int limit) {
        FeatureFlagEntity flag = findFlag(appId, flagKey);
        int pageSize = Math.min(Math.max(limit, 1), MAX_PAGE);
        List<FlagVersionEntity> versions = flagVersionRepository.findByFlag_IdAndEnvironmentOrderByFlagVersionDesc(
                flag.getId(), environment, PageRequest.of(0, pageSize));
        List<FlagVersionSummaryResponse> data = versions.stream()
                .map(v -> new FlagVersionSummaryResponse(
                        v.getFlagVersion(),
                        v.getConfigVersion(),
                        v.getEnvironment(),
                        v.getPublishedBy(),
                        v.getPublishedAt(),
                        v.getComment()))
                .toList();
        return new PageResponse<>(data, PageResponse.Pagination.of(null, false, data.size()));
    }

    @Transactional(readOnly = true)
    public FlagVersionDetailResponse getVersion(String appId, String flagKey, int version, String environment) {
        FeatureFlagEntity flag = findFlag(appId, flagKey);
        FlagVersionEntity entity = flagVersionRepository
                .findByFlag_IdAndEnvironmentAndFlagVersion(flag.getId(), environment, version)
                .orElseThrow(() -> new FmsException(FmsErrorCode.VERSION_NOT_FOUND, "Flag version not found."));
        return new FlagVersionDetailResponse(
                entity.getFlagVersion(),
                entity.getConfigVersion(),
                entity.getEnvironment(),
                entity.getSnapshot(),
                entity.getPublishedBy(),
                entity.getPublishedAt(),
                entity.getComment());
    }

    @Transactional
    public PublishFlagResponse publishFlag(
            String appId, String flagKey, PublishFlagRequest request, String actor, String requestId) {
        FeatureFlagEntity flag = findFlag(appId, flagKey);
        if (flag.getStatus() == FlagStatus.archived) {
            throw new FmsException(FmsErrorCode.VALIDATION_ERROR, "Cannot publish archived flag.");
        }
        return publishOrchestrator.publish(flag, appId, request, actor, requestId);
    }

    @Transactional
    public PublishFlagResponse rollbackFlag(String flagKey, RollbackFlagRequest request, String actor, String requestId) {
        FeatureFlagEntity flag = findFlag(request.appId(), flagKey);
        return publishOrchestrator.rollback(flag, request, actor, requestId);
    }

    FeatureFlagEntity findFlag(String appId, String flagKey) {
        return featureFlagRepository.findByApplication_SlugAndKey(appId, flagKey)
                .orElseThrow(() -> new FmsException(FmsErrorCode.FLAG_NOT_FOUND, "Flag not found: " + flagKey));
    }

    private FlagDetailResponse toDetailResponse(FeatureFlagEntity flag) {
        List<String> tags = featureFlagRepository.findTagNamesByFlagId(flag.getId());
        List<FlagEnvironmentStateEntity> states = flagEnvironmentStateRepository.findByFlag_Id(flag.getId());
        Map<String, Long> configVersions = environmentConfigRepository.findAll().stream()
                .collect(Collectors.toMap(
                        cfg -> cfg.getEnvironment(),
                        cfg -> cfg.getCurrentConfigVersion()));

        List<com.fms.management.dto.EnvironmentStateResponse> environmentStates = states.stream()
                .map(state -> new com.fms.management.dto.EnvironmentStateResponse(
                        state.getEnvironment(),
                        state.isPublished(),
                        state.getLatestVersionId() == null ? null : configVersions.get(state.getEnvironment()),
                        state.isDraftDirty()))
                .toList();

        Map<String, List<com.fms.management.dto.RuleResponse>> rulesByEnv = new LinkedHashMap<>();
        for (String env : List.of("dev", "staging", "prod")) {
            List<FlagRuleEntity> rules = flagRuleRepository.findByFlag_IdAndEnvironmentOrderByPriorityAsc(flag.getId(), env);
            if (!rules.isEmpty()) {
                rulesByEnv.put(env, rules.stream().map(this::toRuleResponse).toList());
            }
        }

        return new FlagDetailResponse(
                flag.getId(),
                flag.getApplication().getSlug(),
                flag.getKey(),
                flag.getName(),
                flag.getDescription(),
                flag.getType().externalName(),
                flag.getDefaultValue(),
                flag.getStatus(),
                tags,
                environmentStates,
                rulesByEnv,
                flag.getCreatedAt(),
                flag.getUpdatedAt(),
                flag.getCreatedBy());
    }

    private FlagSummaryResponse toSummary(FeatureFlagEntity flag) {
        return new FlagSummaryResponse(
                flag.getApplication().getSlug(),
                flag.getKey(),
                flag.getName(),
                flag.getType().externalName(),
                flag.getStatus(),
                flag.getUpdatedAt());
    }

    private com.fms.management.dto.RuleResponse toRuleResponse(FlagRuleEntity rule) {
        return new com.fms.management.dto.RuleResponse(
                rule.getId(),
                rule.getPriority(),
                rule.getName(),
                rule.getConditions(),
                rule.getValue(),
                rule.isEnabled());
    }

    private void linkTags(FeatureFlagEntity flag, List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return;
        }
        for (String raw : tags) {
            String normalized = raw.trim().toLowerCase();
            TagEntity tag = tagRepository.findByName(normalized)
                    .orElseGet(() -> {
                        TagEntity created = new TagEntity();
                        created.setName(normalized);
                        return tagRepository.save(created);
                    });
            featureFlagRepository.linkTag(flag.getId(), tag.getName());
        }
    }

    private void markAllEnvironmentsDirty(FeatureFlagEntity flag) {
        flagEnvironmentStateRepository.findByFlag_Id(flag.getId()).forEach(state -> {
            state.setDraftDirty(true);
            flagEnvironmentStateRepository.save(state);
        });
    }

    private void validateDefaultValue(String type, Object defaultValue) {
        FlagType flagType = FlagType.fromExternal(type);
        switch (flagType) {
            case boolean_ -> {
                if (!(defaultValue instanceof Boolean)) {
                    throw new FmsException(FmsErrorCode.VALIDATION_ERROR, "defaultValue must be boolean.");
                }
            }
            case string -> {
                if (!(defaultValue instanceof String)) {
                    throw new FmsException(FmsErrorCode.VALIDATION_ERROR, "defaultValue must be string.");
                }
            }
            case number -> {
                if (!(defaultValue instanceof Number)) {
                    throw new FmsException(FmsErrorCode.VALIDATION_ERROR, "defaultValue must be number.");
                }
            }
            case json -> {
                if (!(defaultValue instanceof Map<?, ?>)) {
                    throw new FmsException(FmsErrorCode.VALIDATION_ERROR, "defaultValue must be JSON object.");
                }
            }
        }
    }

    private String generateRolloutSalt() {
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
