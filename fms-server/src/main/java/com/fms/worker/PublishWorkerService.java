package com.fms.worker;

import com.fms.cache.SnapshotCacheService;
import com.fms.config.FmsWorkerProperties;
import com.fms.domain.FeatureFlagEntity;
import com.fms.domain.PublishJobEntity;
import com.fms.domain.enums.PublishJobStatus;
import com.fms.observability.FmsMetrics;
import com.fms.repository.PublishJobRepository;
import com.fms.sync.dto.SnapshotResponse;
import com.fms.sync.service.SnapshotLoaderService;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class PublishWorkerService {

    private static final Logger log = LoggerFactory.getLogger(PublishWorkerService.class);
    private static final int BATCH_SIZE = 10;

    private final PublishJobRepository publishJobRepository;
    private final SnapshotLoaderService snapshotLoaderService;
    private final SnapshotCacheService snapshotCacheService;
    private final FmsMetrics metrics;
    private final FmsWorkerProperties workerProperties;

    public PublishWorkerService(
            PublishJobRepository publishJobRepository,
            SnapshotLoaderService snapshotLoaderService,
            SnapshotCacheService snapshotCacheService,
            FmsMetrics metrics,
            FmsWorkerProperties workerProperties) {
        this.publishJobRepository = publishJobRepository;
        this.snapshotLoaderService = snapshotLoaderService;
        this.snapshotCacheService = snapshotCacheService;
        this.metrics = metrics;
        this.workerProperties = workerProperties;
    }

    @Observed(name = "fms.publish.worker", contextualName = "process-pending-jobs")
    @Transactional
    public int processPendingJobs() {
        Instant now = Instant.now();
        List<Long> claimedIds = publishJobRepository.claimJobIds(
                workerProperties.instanceId(), now, BATCH_SIZE);
        if (claimedIds.isEmpty()) {
            return 0;
        }

        List<PublishJobEntity> jobs = publishJobRepository.findAllByIdInWithFlag(claimedIds);
        int processed = 0;
        for (PublishJobEntity job : jobs) {
            if (processJob(job)) {
                processed++;
            }
        }
        return processed;
    }

    private boolean processJob(PublishJobEntity job) {
        long start = System.nanoTime();
        FeatureFlagEntity flag = job.getFlag();
        String appId = flag.getApplication().getSlug();
        String environment = job.getEnvironment();
        long configVersion = job.getConfigVersion();
        long previousVersion = configVersion - 1;

        if (snapshotCacheService.isSnapshotCached(environment, appId, configVersion)) {
            markCompleted(job);
            publishJobRepository.save(job);
            metrics.recordPublishDuration(System.nanoTime() - start, environment, "cached");
            return true;
        }

        try {
            SnapshotResponse snapshot = snapshotLoaderService.compileFullSnapshot(
                    environment, appId, configVersion);
            snapshotCacheService.storeSnapshot(snapshot);

            if (previousVersion > 0) {
                SnapshotResponse delta = snapshotLoaderService.loadDeltaSnapshot(
                        environment, appId, previousVersion, configVersion);
                snapshotCacheService.storeDelta(delta);
            }

            snapshotCacheService.publishVersionChange(environment, appId, configVersion, previousVersion);
            markCompleted(job);
            publishJobRepository.save(job);
            log.info("Processed publish job {} for {}/{} at version {}",
                    job.getId(), appId, environment, configVersion);
            metrics.recordPublishDuration(System.nanoTime() - start, environment, "success");
            return true;
        } catch (RuntimeException ex) {
            log.error("Failed to process publish job {}", job.getId(), ex);
            markFailed(job, ex);
            publishJobRepository.save(job);
            metrics.recordPublishDuration(System.nanoTime() - start, environment, "failed");
            return false;
        }
    }

    private void markCompleted(PublishJobEntity job) {
        job.setStatus(PublishJobStatus.completed);
        job.setCompletedAt(Instant.now());
        job.setLockedBy(null);
        job.setLockedAt(null);
        job.setErrorMessage(null);
    }

    private void markFailed(PublishJobEntity job, RuntimeException ex) {
        job.setStatus(PublishJobStatus.failed);
        job.setLockedBy(null);
        job.setLockedAt(null);
        job.setErrorMessage(sanitizeErrorMessage(ex.getMessage()));
        if (job.getAttemptCount() < job.getMaxAttempts()) {
            job.setNextRetryAt(Instant.now().plus(retryDelay(job.getAttemptCount())));
        }
    }

    private Duration retryDelay(short attemptCount) {
        Duration initial = workerProperties.initialRetryDelay();
        Duration max = workerProperties.maxRetryDelay();
        long multiplier = 1L << Math.max(0, attemptCount - 1);
        Duration delay = initial.multipliedBy(multiplier);
        return delay.compareTo(max) > 0 ? max : delay;
    }

    private static String sanitizeErrorMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Publish job failed";
        }
        return message.length() > 2000 ? message.substring(0, 2000) : message;
    }
}
