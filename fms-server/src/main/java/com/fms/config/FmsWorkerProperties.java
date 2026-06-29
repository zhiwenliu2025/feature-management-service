package com.fms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.UUID;

@ConfigurationProperties(prefix = "fms.worker.publish")
public class FmsWorkerProperties {

    private long pollIntervalMs = 5_000L;
    private String instanceId;
    private Duration initialRetryDelay = Duration.ofSeconds(5);
    private Duration maxRetryDelay = Duration.ofMinutes(5);

    public long pollIntervalMs() {
        return pollIntervalMs;
    }

    public void setPollIntervalMs(long pollIntervalMs) {
        this.pollIntervalMs = pollIntervalMs;
    }

    public String instanceId() {
        if (instanceId == null || instanceId.isBlank()) {
            instanceId = "worker-" + UUID.randomUUID();
        }
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public Duration initialRetryDelay() {
        return initialRetryDelay;
    }

    public void setInitialRetryDelay(Duration initialRetryDelay) {
        this.initialRetryDelay = initialRetryDelay;
    }

    public Duration maxRetryDelay() {
        return maxRetryDelay;
    }

    public void setMaxRetryDelay(Duration maxRetryDelay) {
        this.maxRetryDelay = maxRetryDelay;
    }
}
