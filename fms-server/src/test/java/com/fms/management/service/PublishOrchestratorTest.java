package com.fms.management.service;

import com.fms.common.exception.FmsErrorCode;
import com.fms.common.exception.FmsException;
import com.fms.domain.FeatureFlagEntity;
import com.fms.domain.enums.PublishJobStatus;
import com.fms.management.dto.PublishFlagRequest;
import com.fms.management.support.AuditRecorder;
import com.fms.repository.ConfigVersionHistoryRepository;
import com.fms.repository.EnvironmentConfigRepository;
import com.fms.repository.EnvironmentRepository;
import com.fms.repository.FlagEnvironmentStateRepository;
import com.fms.repository.FlagRuleRepository;
import com.fms.repository.FlagVersionRepository;
import com.fms.repository.PublishJobRepository;
import com.fms.repository.ReleaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublishOrchestratorTest {

    @Mock
    private EnvironmentRepository environmentRepository;
    @Mock
    private EnvironmentConfigRepository environmentConfigRepository;
    @Mock
    private FlagRuleRepository flagRuleRepository;
    @Mock
    private FlagVersionRepository flagVersionRepository;
    @Mock
    private FlagEnvironmentStateRepository flagEnvironmentStateRepository;
    @Mock
    private PublishJobRepository publishJobRepository;
    @Mock
    private ConfigVersionHistoryRepository configVersionHistoryRepository;
    @Mock
    private ReleaseRepository releaseRepository;
    @Mock
    private AuditRecorder auditRecorder;

    private PublishOrchestrator publishOrchestrator;

    @BeforeEach
    void setUp() {
        publishOrchestrator = new PublishOrchestrator(
                environmentRepository,
                environmentConfigRepository,
                flagRuleRepository,
                flagVersionRepository,
                flagEnvironmentStateRepository,
                publishJobRepository,
                configVersionHistoryRepository,
                releaseRepository,
                auditRecorder);
    }

    @Test
    void publishRejectsWhenJobAlreadyPending() {
        UUID flagId = UUID.randomUUID();
        FeatureFlagEntity flag = mock(FeatureFlagEntity.class);
        when(flag.getId()).thenReturn(flagId);

        when(environmentRepository.existsById("prod")).thenReturn(true);
        when(publishJobRepository.existsByFlag_IdAndEnvironmentAndStatus(
                flagId, "prod", PublishJobStatus.pending)).thenReturn(true);

        PublishFlagRequest request = new PublishFlagRequest("prod", null, "publish", false);

        FmsException ex = assertThrows(
                FmsException.class,
                () -> publishOrchestrator.publish(flag, "checkout-service", request, "actor", "req-1"));

        assertEquals(FmsErrorCode.PUBLISH_IN_PROGRESS, ex.errorCode());
        verify(flagRuleRepository, never()).findByFlag_IdAndEnvironmentOrderByPriorityAsc(any(), any());
        verify(publishJobRepository, never()).save(any());
    }
}
