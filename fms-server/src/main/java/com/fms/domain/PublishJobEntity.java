package com.fms.domain;

import com.fms.domain.enums.PublishJobStatus;
import com.fms.domain.enums.PublishJobType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "publish_jobs")
public class PublishJobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flag_id", nullable = false)
    private FeatureFlagEntity flag;

    @Column(name = "flag_key", nullable = false, length = 128)
    private String flagKey;

    @Column(nullable = false, length = 32)
    private String environment;

    @Column(name = "config_version", nullable = false)
    private long configVersion;

    @Column(name = "flag_version_id")
    private Long flagVersionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 32)
    private PublishJobType jobType = PublishJobType.publish;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Object payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PublishJobStatus status = PublishJobStatus.pending;

    @Column(name = "attempt_count", nullable = false)
    private short attemptCount;

    @Column(name = "max_attempts", nullable = false)
    private short maxAttempts = 5;

    @Column(name = "locked_by", length = 128)
    private String lockedBy;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "next_retry_at", nullable = false)
    private Instant nextRetryAt = Instant.now();

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    public Long getId() {
        return id;
    }

    public FeatureFlagEntity getFlag() {
        return flag;
    }

    public void setFlag(FeatureFlagEntity flag) {
        this.flag = flag;
    }

    public String getFlagKey() {
        return flagKey;
    }

    public void setFlagKey(String flagKey) {
        this.flagKey = flagKey;
    }

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

    public Long getFlagVersionId() {
        return flagVersionId;
    }

    public void setFlagVersionId(Long flagVersionId) {
        this.flagVersionId = flagVersionId;
    }

    public PublishJobType getJobType() {
        return jobType;
    }

    public void setJobType(PublishJobType jobType) {
        this.jobType = jobType;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public PublishJobStatus getStatus() {
        return status;
    }

    public void setStatus(PublishJobStatus status) {
        this.status = status;
    }

    public short getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(short attemptCount) {
        this.attemptCount = attemptCount;
    }

    public short getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(short maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public String getLockedBy() {
        return lockedBy;
    }

    public void setLockedBy(String lockedBy) {
        this.lockedBy = lockedBy;
    }

    public Instant getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(Instant lockedAt) {
        this.lockedAt = lockedAt;
    }

    public Instant getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(Instant nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
