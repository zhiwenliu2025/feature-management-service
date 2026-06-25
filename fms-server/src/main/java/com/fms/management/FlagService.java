package com.fms.management;

import com.fms.common.api.PageResponse;
import com.fms.common.exception.FmsErrorCode;
import com.fms.common.exception.FmsException;
import com.fms.domain.ApplicationEntity;
import com.fms.domain.FeatureFlagEntity;
import com.fms.domain.enums.FlagStatus;
import com.fms.domain.enums.FlagType;
import com.fms.management.dto.CreateFlagRequest;
import com.fms.management.dto.FlagResponse;
import com.fms.management.dto.PublishFlagRequest;
import com.fms.management.dto.PublishFlagResponse;
import com.fms.repository.ApplicationRepository;
import com.fms.repository.FeatureFlagRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

@Service
public class FlagService {

    private final ApplicationRepository applicationRepository;
    private final FeatureFlagRepository featureFlagRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public FlagService(ApplicationRepository applicationRepository, FeatureFlagRepository featureFlagRepository) {
        this.applicationRepository = applicationRepository;
        this.featureFlagRepository = featureFlagRepository;
    }

    @Transactional
    public FlagResponse createFlag(CreateFlagRequest request, String actor) {
        ApplicationEntity application = applicationRepository.findBySlug(request.appId())
                .orElseThrow(() -> new FmsException(FmsErrorCode.APPLICATION_NOT_FOUND,
                        "Application not found: " + request.appId()));

        if (featureFlagRepository.existsByApplication_SlugAndKey(request.appId(), request.key())) {
            throw new FmsException(FmsErrorCode.FLAG_ALREADY_EXISTS,
                    "Flag already exists for application.");
        }

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
        return toResponse(saved, request.tags() == null ? List.of() : request.tags());
    }

    @Transactional(readOnly = true)
    public FlagResponse getFlag(String appId, String flagKey) {
        FeatureFlagEntity flag = findFlag(appId, flagKey);
        return toResponse(flag, List.of());
    }

    @Transactional(readOnly = true)
    public PageResponse<FlagResponse> listFlags(String appId, FlagStatus status, int limit) {
        int pageSize = Math.min(Math.max(limit, 1), 100);
        var page = status == null
                ? featureFlagRepository.findByApplication_Slug(appId, PageRequest.of(0, pageSize))
                : featureFlagRepository.findByApplication_SlugAndStatus(appId, status, PageRequest.of(0, pageSize));

        List<FlagResponse> data = page.getContent().stream()
                .map(flag -> toResponse(flag, List.of()))
                .toList();

        return new PageResponse<>(data, PageResponse.Pagination.of(null, page.hasNext(), page.getTotalElements()));
    }

    @Transactional
    public PublishFlagResponse publishFlag(String appId, String flagKey, PublishFlagRequest request, String actor) {
        findFlag(appId, flagKey);
        // Scaffold: publish pipeline (Outbox + Redis) to be implemented
        return new PublishFlagResponse(
                flagKey,
                appId,
                request.environment(),
                1L,
                1,
                "pending",
                "pending",
                Instant.now(),
                actor);
    }

    private FeatureFlagEntity findFlag(String appId, String flagKey) {
        return featureFlagRepository.findByApplication_SlugAndKey(appId, flagKey)
                .orElseThrow(() -> new FmsException(FmsErrorCode.FLAG_NOT_FOUND,
                        "Flag not found: " + flagKey));
    }

    private FlagResponse toResponse(FeatureFlagEntity flag, List<String> tags) {
        return new FlagResponse(
                flag.getId(),
                flag.getApplication().getSlug(),
                flag.getKey(),
                flag.getName(),
                flag.getDescription(),
                flag.getType().externalName(),
                flag.getDefaultValue(),
                flag.getStatus(),
                tags,
                flag.getCreatedAt(),
                flag.getCreatedBy());
    }

    private String generateRolloutSalt() {
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
