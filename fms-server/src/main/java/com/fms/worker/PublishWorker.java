package com.fms.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PublishWorker {

    private static final Logger log = LoggerFactory.getLogger(PublishWorker.class);

    private final PublishWorkerService publishWorkerService;

    public PublishWorker(PublishWorkerService publishWorkerService) {
        this.publishWorkerService = publishWorkerService;
    }

    @Scheduled(fixedDelayString = "${fms.worker.publish.poll-interval-ms:5000}")
    public void pollPublishJobs() {
        int processed = publishWorkerService.processPendingJobs();
        if (processed > 0) {
            log.debug("Processed {} publish job(s)", processed);
        }
    }
}
