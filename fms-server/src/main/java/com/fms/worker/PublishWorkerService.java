package com.fms.worker;

import com.fms.cache.SnapshotCacheService;
import com.fms.domain.FeatureFlagEntity;
import com.fms.domain.PublishJobEntity;
import com.fms.domain.enums.PublishJobStatus;
import com.fms.observability.FmsMetrics;
import com.fms.repository.PublishJobRepository;
import com.fms.sync.service.SnapshotLoaderService;
import com.fms.sync.dto.SnapshotResponse;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PublishWorkerService {

    private static final Logger log = LoggerFactory.getLogger(PublishWorkerService.class);
    private static final int BATCH_SIZE = 10;

    private final PublishJobRepository publishJobRepository;
    private final SnapshotLoaderService snapshotLoaderService;
    private final SnapshotCacheService snapshotCacheService;
    private final FmsMetrics metrics;

    public PublishWorkerService(
            PublishJobRepository publishJobRepository,
            SnapshotLoaderService snapshotLoaderService,
            SnapshotCacheService snapshotCacheService,
            FmsMetrics metrics) {
        this.publishJobRepository = publishJobRepository;
        this.snapshotLoaderService = snapshotLoaderService;
        this.snapshotCacheService = snapshotCacheService;
        this.metrics = metrics;
    }

    @Observed(name = "fms.publish.worker", contextualName = "process-pending-jobs")
    @Transactional
    public int processPendingJobs() {
        List<PublishJobEntity> jobs = publishJobRepository.findByStatusOrderByCreatedAtAsc(
                PublishJobStatus.pending, PageRequest.of(0, BATCH_SIZE));
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
            job.setStatus(PublishJobStatus.completed);
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
            job.setStatus(PublishJobStatus.completed);
            publishJobRepository.save(job);
            log.info("Processed publish job {} for {}/{} at version {}",
                    job.getId(), appId, environment, configVersion);
            metrics.recordPublishDuration(System.nanoTime() - start, environment, "success");
            return true;
        } catch (RuntimeException ex) {
            log.error("Failed to process publish job {}", job.getId(), ex);
            job.setStatus(PublishJobStatus.failed);
            publishJobRepository.save(job);
            metrics.recordPublishDuration(System.nanoTime() - start, environment, "failed");
            return false;
        }
    }
}
