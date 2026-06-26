package com.fms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "fms.sync")
public class FmsSyncProperties {

    private int deltaMaxGap = 100;
    private Duration snapshotTtl = Duration.ofHours(24);
    private Duration sseHeartbeatInterval = Duration.ofSeconds(30);
    private long sseHeartbeatIntervalMs = 30_000L;
    private Duration sseTimeout = Duration.ofHours(1);

    public int deltaMaxGap() {
        return deltaMaxGap;
    }

    public void setDeltaMaxGap(int deltaMaxGap) {
        this.deltaMaxGap = deltaMaxGap;
    }

    public Duration snapshotTtl() {
        return snapshotTtl;
    }

    public void setSnapshotTtl(Duration snapshotTtl) {
        this.snapshotTtl = snapshotTtl;
    }

    public Duration sseHeartbeatInterval() {
        return sseHeartbeatInterval;
    }

    public void setSseHeartbeatInterval(Duration sseHeartbeatInterval) {
        this.sseHeartbeatInterval = sseHeartbeatInterval;
        this.sseHeartbeatIntervalMs = sseHeartbeatInterval.toMillis();
    }

    public long sseHeartbeatIntervalMs() {
        return sseHeartbeatIntervalMs;
    }

    public void setSseHeartbeatIntervalMs(long sseHeartbeatIntervalMs) {
        this.sseHeartbeatIntervalMs = sseHeartbeatIntervalMs;
    }

    public Duration sseTimeout() {
        return sseTimeout;
    }

    public void setSseTimeout(Duration sseTimeout) {
        this.sseTimeout = sseTimeout;
    }
}
