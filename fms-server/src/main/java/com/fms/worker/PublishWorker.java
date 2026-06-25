package com.fms.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls PostgreSQL {@code publish_jobs} Outbox and writes compiled snapshots to Redis.
 * Full implementation pending.
 */
@Component
public class PublishWorker {

    private static final Logger log = LoggerFactory.getLogger(PublishWorker.class);

    @Scheduled(fixedDelayString = "${fms.worker.publish.poll-interval-ms:5000}")
    public void pollPublishJobs() {
        log.debug("Publish worker poll tick — outbox processing not yet implemented");
    }
}
