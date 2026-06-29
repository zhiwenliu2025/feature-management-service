package com.fms.management.service;

import com.fms.common.api.PageResponse;
import com.fms.domain.AuditEventEntity;
import com.fms.domain.enums.AuditAction;
import com.fms.management.dto.AuditEventResponse;
import com.fms.management.support.CursorCodec;
import com.fms.repository.AuditEventRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AuditQueryService {

    private final AuditEventRepository auditEventRepository;

    public AuditQueryService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditEventResponse> query(
            String resourceType,
            String resourceId,
            String actor,
            AuditAction action,
            String environment,
            Instant from,
            Instant to,
            int limit) {
        int pageSize = Math.min(Math.max(limit, 1), 100);
        Specification<AuditEventEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (resourceType != null) {
                predicates.add(cb.equal(root.get("resourceType"), resourceType));
            }
            if (resourceId != null) {
                predicates.add(cb.equal(root.get("resourceId"), resourceId));
            }
            if (actor != null) {
                predicates.add(cb.equal(root.get("actor"), actor));
            }
            if (action != null) {
                predicates.add(cb.equal(root.get("action"), action));
            }
            if (environment != null) {
                predicates.add(cb.equal(root.get("environment"), environment));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };

        var page = auditEventRepository.findAll(
                spec,
                PageRequest.of(0, pageSize + 1, Sort.by(Sort.Direction.DESC, "createdAt", "id")));

        List<AuditEventEntity> events = new ArrayList<>(page.getContent());
        boolean hasMore = events.size() > pageSize;
        if (hasMore) {
            events = events.subList(0, pageSize);
        }

        List<AuditEventResponse> data = events.stream().map(this::toResponse).toList();
        String nextCursor = hasMore && !events.isEmpty()
                ? CursorCodec.encode(events.getLast().getCreatedAt(), java.util.UUID.randomUUID())
                : null;
        return new PageResponse<>(data, PageResponse.Pagination.of(nextCursor, hasMore, data.size()));
    }

    @SuppressWarnings("unchecked")
    private AuditEventResponse toResponse(AuditEventEntity event) {
        Map<String, Object> diff = event.getDiff() instanceof Map<?, ?> diffMap
                ? (Map<String, Object>) diffMap
                : Map.of();
        Map<String, Object> metadata = event.getMetadata() instanceof Map<?, ?> metadataMap
                ? (Map<String, Object>) metadataMap
                : Map.of();
        return new AuditEventResponse(
                String.valueOf(event.getId()),
                event.getActor(),
                event.getAction(),
                event.getResourceType(),
                event.getResourceId(),
                event.getEnvironment(),
                diff,
                metadata,
                event.getCreatedAt());
    }
}
