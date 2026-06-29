package com.fms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fms.observability")
public class FmsObservabilityProperties {

    private boolean enabled = true;
    private long metricsPollIntervalMs = 30_000L;

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long metricsPollIntervalMs() {
        return metricsPollIntervalMs;
    }

    public void setMetricsPollIntervalMs(long metricsPollIntervalMs) {
        this.metricsPollIntervalMs = metricsPollIntervalMs;
    }
}
