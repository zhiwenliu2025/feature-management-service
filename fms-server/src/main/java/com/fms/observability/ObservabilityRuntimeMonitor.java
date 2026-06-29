package com.fms.observability;

import com.fms.config.FmsObservabilityProperties;
import com.fms.domain.PublishJobEntity;
import com.fms.domain.enums.PublishJobStatus;
import com.fms.repository.PublishJobRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "fms.observability", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ObservabilityRuntimeMonitor {

    private final PublishJobRepository publishJobRepository;
    private final FmsMetrics metrics;
    private final FmsObservabilityProperties properties;

    public ObservabilityRuntimeMonitor(
            PublishJobRepository publishJobRepository,
            FmsMetrics metrics,
            FmsObservabilityProperties properties) {
        this.publishJobRepository = publishJobRepository;
        this.metrics = metrics;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${fms.observability.metrics-poll-interval-ms:30000}")
    void refreshRuntimeGauges() {
        if (!properties.enabled()) {
            return;
        }

        long pending = publishJobRepository.countByStatus(PublishJobStatus.pending);
        long failed = publishJobRepository.countByStatus(PublishJobStatus.failed);
        metrics.setPublishBacklog(pending + failed);

        for (String environment : List.of("dev", "staging", "prod")) {
            long lagSeconds = publishJobRepository
                    .findByStatusAndEnvironmentOrderByCreatedAtAsc(
                            PublishJobStatus.pending, environment, PageRequest.of(0, 1))
                    .stream()
                    .findFirst()
                    .map(this::lagSeconds)
                    .orElse(0L);
            metrics.setConfigVersionLag(environment, lagSeconds);
        }
    }

    private long lagSeconds(PublishJobEntity job) {
        return Math.max(0, Duration.between(job.getCreatedAt(), Instant.now()).toSeconds());
    }
}
