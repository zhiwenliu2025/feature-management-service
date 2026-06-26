package com.fms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "environment_config")
public class EnvironmentConfigEntity {

    @Id
    @Column(length = 32)
    private String environment;

    @Column(name = "current_config_version", nullable = false)
    private long currentConfigVersion;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public String getEnvironment() {
        return environment;
    }

    public long getCurrentConfigVersion() {
        return currentConfigVersion;
    }

    public void setCurrentConfigVersion(long currentConfigVersion) {
        this.currentConfigVersion = currentConfigVersion;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
