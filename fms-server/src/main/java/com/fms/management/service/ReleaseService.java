package com.fms.management;

import com.fms.common.api.PageResponse;
import com.fms.common.exception.FmsErrorCode;
import com.fms.common.exception.FmsException;
import com.fms.domain.FeatureFlagEntity;
import com.fms.domain.ReleaseEntity;
import com.fms.domain.ReleaseFlagEntity;
import com.fms.domain.enums.AuditAction;
import com.fms.management.dto.CreateReleaseRequest;
import com.fms.management.dto.LinkFlagsRequest;
import com.fms.management.dto.ReleaseDetailResponse;
import com.fms.management.dto.ReleaseResponse;
import com.fms.management.support.AuditRecorder;
import com.fms.repository.ReleaseFlagRepository;
import com.fms.repository.ReleaseRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class ReleaseService {

    private final ReleaseRepository releaseRepository;
    private final ReleaseFlagRepository releaseFlagRepository;
    private final FlagService flagService;
    private final AuditRecorder auditRecorder;

    public ReleaseService(
            ReleaseRepository releaseRepository,
            ReleaseFlagRepository releaseFlagRepository,
            FlagService flagService,
            AuditRecorder auditRecorder) {
        this.releaseRepository = releaseRepository;
        this.releaseFlagRepository = releaseFlagRepository;
        this.flagService = flagService;
        this.auditRecorder = auditRecorder;
    }

    @Transactional
    public ReleaseResponse createRelease(CreateReleaseRequest request, String actor, String requestId) {
        if (releaseRepository.existsByReleaseId(request.releaseId())) {
            throw new FmsException(FmsErrorCode.RELEASE_ALREADY_EXISTS, "Release already exists.");
        }
        ReleaseEntity release = new ReleaseEntity();
        release.setReleaseId(request.releaseId());
        release.setVersion(request.version());
        release.setTitle(request.title());
        release.setDescription(request.description());
        if (request.metadata() != null) {
            release.setMetadata(request.metadata());
        }
        release.setCreatedBy(actor);
        ReleaseEntity saved = releaseRepository.save(release);
        auditRecorder.record(actor, AuditAction.create, "release", saved.getReleaseId(), null, requestId, Map.of());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReleaseResponse> listReleases(int limit) {
        int pageSize = Math.min(Math.max(limit, 1), 100);
        var page = releaseRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, pageSize));
        List<ReleaseResponse> data = page.getContent().stream().map(this::toResponse).toList();
        return new PageResponse<>(data, PageResponse.Pagination.of(null, page.hasNext(), page.getTotalElements()));
    }

    @Transactional(readOnly = true)
    public ReleaseDetailResponse getRelease(String releaseId) {
        ReleaseEntity release = findRelease(releaseId);
        List<ReleaseDetailResponse.LinkedFlagResponse> flags = releaseFlagRepository.findByRelease_ReleaseId(releaseId)
                .stream()
                .map(link -> new ReleaseDetailResponse.LinkedFlagResponse(
                        link.getFlag().getApplication().getSlug(),
                        link.getFlag().getKey(),
                        link.getEnvironment(),
                        link.getConfigVersion()))
                .toList();
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = release.getMetadata() instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();
        return new ReleaseDetailResponse(
                release.getId(),
                release.getReleaseId(),
                release.getVersion(),
                release.getTitle(),
                release.getDescription(),
                metadata,
                flags,
                release.getCreatedAt(),
                release.getCreatedBy());
    }

    @Transactional
    public ReleaseDetailResponse linkFlags(String releaseId, LinkFlagsRequest request, String actor, String requestId) {
        ReleaseEntity release = findRelease(releaseId);
        for (String flagKey : request.flagKeys()) {
            FeatureFlagEntity flag = flagService.findFlag(request.appId(), flagKey);
            ReleaseFlagEntity link = new ReleaseFlagEntity();
            link.setRelease(release);
            link.setFlag(flag);
            link.setEnvironment(request.environment());
            releaseFlagRepository.save(link);
        }
        auditRecorder.record(actor, AuditAction.update, "release", releaseId, request.environment(), requestId,
                Map.of("flagKeys", request.flagKeys()));
        return getRelease(releaseId);
    }

    private ReleaseEntity findRelease(String releaseId) {
        return releaseRepository.findByReleaseId(releaseId)
                .orElseThrow(() -> new FmsException(FmsErrorCode.RELEASE_NOT_FOUND,
                        "Release not found: " + releaseId));
    }

    private ReleaseResponse toResponse(ReleaseEntity release) {
        return new ReleaseResponse(
                release.getId(),
                release.getReleaseId(),
                release.getVersion(),
                release.getTitle(),
                release.getCreatedAt(),
                release.getCreatedBy());
    }
}
