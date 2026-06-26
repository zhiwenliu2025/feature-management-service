package com.fms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "config_version_history")
public class ConfigVersionHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String environment;

    @Column(name = "config_version", nullable = false)
    private long configVersion;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "changed_flag_ids", nullable = false, columnDefinition = "uuid[]")
    private UUID[] changedFlagIds;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "deleted_flag_keys", nullable = false, columnDefinition = "text[]")
    private String[] deletedFlagKeys = new String[0];

    @Column(name = "publish_job_id")
    private Long publishJobId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public long getConfigVersion() {
        return configVersion;
    }

    public void setConfigVersion(long configVersion) {
        this.configVersion = configVersion;
    }

    public UUID[] getChangedFlagIds() {
        return changedFlagIds;
    }

    public void setChangedFlagIds(UUID[] changedFlagIds) {
        this.changedFlagIds = changedFlagIds;
    }

    public String[] getDeletedFlagKeys() {
        return deletedFlagKeys;
    }

    public void setDeletedFlagKeys(String[] deletedFlagKeys) {
        this.deletedFlagKeys = deletedFlagKeys;
    }

    public Long getPublishJobId() {
        return publishJobId;
    }

    public void setPublishJobId(Long publishJobId) {
        this.publishJobId = publishJobId;
    }
}
