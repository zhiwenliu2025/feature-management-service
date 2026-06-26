package com.fms.management.support;

import com.fms.domain.AuditEventEntity;
import com.fms.domain.enums.AuditAction;
import com.fms.repository.AuditEventRepository;
import org.springframework.stereotype.Component;

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
        AuditEventEntity event = new AuditEventEntity();
        event.setActor(actor);
        event.setAction(action);
        event.setResourceType(resourceType);
        event.setResourceId(resourceId);
        event.setEnvironment(environment);
        event.setRequestId(requestId);
        event.setDiff(diff);
        auditEventRepository.save(event);
    }
}
