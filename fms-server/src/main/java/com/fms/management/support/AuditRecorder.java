package com.fms.management.support;

import com.fms.domain.AuditEventEntity;
import com.fms.domain.enums.AuditAction;
import com.fms.repository.AuditEventRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

@Component
public class AuditRecorder {

    private final AuditEventRepository auditEventRepository;

    public AuditRecorder(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    public void record(
            String actor,
            AuditAction action,
            String resourceType,
            String resourceId,
            String environment,
            String requestId,
            Map<String, Object> diff) {
        record(actor, action, resourceType, resourceId, environment, requestId, diff, Map.of());
    }

    public void record(
            String actor,
            AuditAction action,
            String resourceType,
            String resourceId,
            String environment,
            String requestId,
            Map<String, Object> diff,
            Map<String, Object> metadata) {
        AuditEventEntity event = new AuditEventEntity();
        event.setActor(actor);
        event.setActorIpHash(resolveActorIpHash());
        event.setAction(action);
        event.setResourceType(resourceType);
        event.setResourceId(resourceId);
        event.setEnvironment(environment);
        event.setRequestId(requestId);
        event.setDiff(diff);
        event.setMetadata(metadata == null ? Map.of() : metadata);
        auditEventRepository.save(event);
    }

    private static String resolveActorIpHash() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        return ActorIpHasher.hash(ActorIpHasher.resolveClientIp(request));
    }

    private static HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }
}
