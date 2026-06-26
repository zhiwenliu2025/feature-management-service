package com.fms.management;

import com.fms.common.api.PageResponse;
import com.fms.common.exception.FmsErrorCode;
import com.fms.common.exception.FmsException;
import com.fms.domain.ApiKeyEntity;
import com.fms.domain.ApplicationEntity;
import com.fms.domain.enums.ApplicationStatus;
import com.fms.domain.enums.AuditAction;
import com.fms.management.dto.ApiKeyCreatedResponse;
import com.fms.management.dto.ApiKeyResponse;
import com.fms.management.dto.ApplicationResponse;
import com.fms.management.dto.CreateApiKeyRequest;
import com.fms.management.dto.CreateApplicationRequest;
import com.fms.management.dto.UpdateApplicationRequest;
import com.fms.management.support.AuditRecorder;
import com.fms.management.support.CursorCodec;
import com.fms.repository.ApiKeyRepository;
import com.fms.repository.ApplicationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ApplicationService {

    private static final List<String> DEFAULT_SCOPES = List.of("sync", "evaluate");

    private final ApplicationRepository applicationRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final AuditRecorder auditRecorder;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public ApplicationService(
            ApplicationRepository applicationRepository,
            ApiKeyRepository apiKeyRepository,
            AuditRecorder auditRecorder,
            PasswordEncoder passwordEncoder) {
        this.applicationRepository = applicationRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.auditRecorder = auditRecorder;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public ApplicationResponse createApplication(CreateApplicationRequest request, String actor, String requestId) {
        if (applicationRepository.existsBySlug(request.slug())) {
            throw new FmsException(FmsErrorCode.VALIDATION_ERROR, "Application slug already exists.");
        }
        ApplicationEntity app = new ApplicationEntity();
        app.setSlug(request.slug());
        app.setName(request.name());
        app.setDescription(request.description());
        app.setOwnerTeam(request.ownerTeam());
        app.setCreatedBy(actor);
        app.setStatus(ApplicationStatus.active);
        ApplicationEntity saved = applicationRepository.save(app);
        auditRecorder.record(actor, AuditAction.create, "application", saved.getSlug(), null, requestId,
                Map.of("after", Map.of("slug", saved.getSlug())));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<ApplicationResponse> listApplications(int limit, String cursor) {
        int pageSize = Math.min(Math.max(limit, 1), 100);
        var page = applicationRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, pageSize + 1));
        List<ApplicationEntity> items = page.getContent();
        boolean hasMore = items.size() > pageSize;
        if (hasMore) {
            items = items.subList(0, pageSize);
        }
        List<ApplicationResponse> data = items.stream().map(this::toResponse).toList();
        String nextCursor = hasMore && !items.isEmpty()
                ? CursorCodec.encode(items.getLast().getCreatedAt(), items.getLast().getId())
                : null;
        return new PageResponse<>(data, PageResponse.Pagination.of(nextCursor, hasMore, page.getTotalElements()));
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getApplication(String appId) {
        return toResponse(findApplication(appId));
    }

    @Transactional
    public ApplicationResponse updateApplication(String appId, UpdateApplicationRequest request, String actor, String requestId) {
        ApplicationEntity app = findApplication(appId);
        if (request.name() != null) {
            app.setName(request.name());
        }
        if (request.description() != null) {
            app.setDescription(request.description());
        }
        if (request.ownerTeam() != null) {
            app.setOwnerTeam(request.ownerTeam());
        }
        ApplicationEntity saved = applicationRepository.save(app);
        auditRecorder.record(actor, AuditAction.update, "application", appId, null, requestId, Map.of());
        return toResponse(saved);
    }

    @Transactional
    public ApiKeyCreatedResponse createApiKey(String appId, CreateApiKeyRequest request, String actor, String requestId) {
        ApplicationEntity app = findApplication(appId);
        String prefix = "fms_" + randomHex(2);
        String secret = randomHex(16);
        String plaintext = prefix + "." + secret;

        ApiKeyEntity key = new ApiKeyEntity();
        key.setApplication(app);
        key.setName(request.name());
        key.setKeyPrefix(prefix);
        key.setKeyHash(passwordEncoder.encode(plaintext));
        key.setScopes(request.scopes() == null || request.scopes().isEmpty() ? DEFAULT_SCOPES : request.scopes());
        key.setExpiresAt(request.expiresAt());
        key.setCreatedBy(actor);
        ApiKeyEntity saved = apiKeyRepository.save(key);

        auditRecorder.record(actor, AuditAction.create, "api_key", saved.getId().toString(), null, requestId,
                Map.of("appId", appId, "keyPrefix", prefix));

        return new ApiKeyCreatedResponse(
                saved.getId(),
                saved.getKeyPrefix(),
                plaintext,
                saved.getScopes(),
                saved.getExpiresAt(),
                saved.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<ApiKeyResponse> listApiKeys(String appId) {
        findApplication(appId);
        return apiKeyRepository.findByApplication_SlugOrderByCreatedAtDesc(appId).stream()
                .map(key -> new ApiKeyResponse(
                        key.getId(),
                        key.getKeyPrefix(),
                        key.getName(),
                        key.getScopes(),
                        key.getExpiresAt(),
                        key.getRevokedAt(),
                        key.getCreatedAt()))
                .toList();
    }

    @Transactional
    public void revokeApiKey(String appId, UUID keyId, String actor, String requestId) {
        findApplication(appId);
        ApiKeyEntity key = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new FmsException(FmsErrorCode.API_KEY_NOT_FOUND, "API key not found."));
        if (!key.getApplication().getSlug().equals(appId)) {
            throw new FmsException(FmsErrorCode.API_KEY_NOT_FOUND, "API key not found.");
        }
        key.setRevokedAt(Instant.now());
        apiKeyRepository.save(key);
        auditRecorder.record(actor, AuditAction.delete, "api_key", keyId.toString(), null, requestId, Map.of());
    }

    private ApplicationEntity findApplication(String appId) {
        return applicationRepository.findBySlug(appId)
                .orElseThrow(() -> new FmsException(FmsErrorCode.APPLICATION_NOT_FOUND,
                        "Application not found: " + appId));
    }

    private ApplicationResponse toResponse(ApplicationEntity app) {
        return new ApplicationResponse(
                app.getId(),
                app.getSlug(),
                app.getName(),
                app.getDescription(),
                app.getStatus(),
                app.getOwnerTeam(),
                app.getCreatedAt(),
                app.getCreatedBy());
    }

    private String randomHex(int bytes) {
        byte[] data = new byte[bytes];
        secureRandom.nextBytes(data);
        return HexFormat.of().formatHex(data);
    }
}
