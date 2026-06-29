package com.fms.worker;

import com.fms.cache.SnapshotCacheService;
import com.fms.config.FmsWorkerProperties;
import com.fms.domain.ApplicationEntity;
import com.fms.domain.FeatureFlagEntity;
import com.fms.domain.PublishJobEntity;
import com.fms.domain.enums.PublishJobStatus;
import com.fms.observability.FmsMetrics;
import com.fms.repository.PublishJobRepository;
import com.fms.sync.service.SnapshotLoaderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublishWorkerServiceTest {

    @Mock
    private PublishJobRepository publishJobRepository;
    @Mock
    private SnapshotLoaderService snapshotLoaderService;
    @Mock
    private SnapshotCacheService snapshotCacheService;
    @Mock
    private FmsMetrics metrics;

    private PublishWorkerService publishWorkerService;

    @BeforeEach
    void setUp() {
        FmsWorkerProperties workerProperties = new FmsWorkerProperties();
        workerProperties.setInstanceId("worker-test");
        workerProperties.setInitialRetryDelay(Duration.ofSeconds(5));
        workerProperties.setMaxRetryDelay(Duration.ofMinutes(5));
        publishWorkerService = new PublishWorkerService(
                publishJobRepository,
                snapshotLoaderService,
                snapshotCacheService,
                metrics,
                workerProperties);
    }

    @Test
    void processPendingJobsMarksJobCompletedWhenSnapshotAlreadyCached() {
        PublishJobEntity job = buildJob();
        when(publishJobRepository.claimJobIds(eq("worker-test"), any(Instant.class), eq(10)))
                .thenReturn(List.of(42L));
        when(publishJobRepository.findAllByIdInWithFlag(List.of(42L))).thenReturn(List.of(job));
        when(snapshotCacheService.isSnapshotCached("dev", "checkout-service", 3L)).thenReturn(true);

        int processed = publishWorkerService.processPendingJobs();

        assertEquals(1, processed);
        ArgumentCaptor<PublishJobEntity> saved = ArgumentCaptor.forClass(PublishJobEntity.class);
        verify(publishJobRepository).save(saved.capture());
        assertEquals(PublishJobStatus.completed, saved.getValue().getStatus());
        assertNotNull(saved.getValue().getCompletedAt());
        verify(snapshotLoaderService, never()).compileFullSnapshot(anyString(), anyString(), anyLong());
    }

    @Test
    void processPendingJobsSchedulesRetryWhenProcessingFails() {
        PublishJobEntity job = buildJob();
        when(publishJobRepository.claimJobIds(eq("worker-test"), any(Instant.class), eq(10)))
                .thenReturn(List.of(42L));
        when(publishJobRepository.findAllByIdInWithFlag(List.of(42L))).thenReturn(List.of(job));
        when(snapshotCacheService.isSnapshotCached("dev", "checkout-service", 3L)).thenReturn(false);
        when(snapshotLoaderService.compileFullSnapshot("dev", "checkout-service", 3L))
                .thenThrow(new RuntimeException("redis unavailable"));

        publishWorkerService.processPendingJobs();

        ArgumentCaptor<PublishJobEntity> saved = ArgumentCaptor.forClass(PublishJobEntity.class);
        verify(publishJobRepository).save(saved.capture());
        PublishJobEntity failed = saved.getValue();
        assertEquals(PublishJobStatus.failed, failed.getStatus());
        assertEquals("redis unavailable", failed.getErrorMessage());
        assertNotNull(failed.getNextRetryAt());
    }

    private static PublishJobEntity buildJob() {
        ApplicationEntity application = new ApplicationEntity();
        application.setSlug("checkout-service");

        FeatureFlagEntity flag = new FeatureFlagEntity();
        flag.setApplication(application);
        flag.setKey("new-checkout");

        PublishJobEntity job = new PublishJobEntity();
        job.setFlag(flag);
        job.setEnvironment("dev");
        job.setConfigVersion(3L);
        job.setAttemptCount((short) 1);
        job.setMaxAttempts((short) 5);
        job.setStatus(PublishJobStatus.processing);
        return job;
    }
}
