package com.fms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "flag_environment_state")
public class FlagEnvironmentStateEntity {

    @EmbeddedId
    private Id id = new Id();

    @MapsId("flagId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flag_id", nullable = false)
    private FeatureFlagEntity flag;

    @Column(name = "is_published", nullable = false)
    private boolean published;

    @Column(name = "latest_version_id")
    private Long latestVersionId;

    @Column(name = "draft_dirty", nullable = false)
    private boolean draftDirty = true;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    @Embeddable
    public static class Id implements Serializable {
        @Column(name = "flag_id")
        private UUID flagId;

        @Column(name = "environment", length = 32)
        private String environment;

        public Id() {
        }

        public Id(UUID flagId, String environment) {
            this.flagId = flagId;
            this.environment = environment;
        }

        public UUID getFlagId() {
            return flagId;
        }

        public void setFlagId(UUID flagId) {
            this.flagId = flagId;
        }

        public String getEnvironment() {
            return environment;
        }

        public void setEnvironment(String environment) {
            this.environment = environment;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Id that)) {
                return false;
            }
            return Objects.equals(flagId, that.flagId) && Objects.equals(environment, that.environment);
        }

        @Override
        public int hashCode() {
            return Objects.hash(flagId, environment);
        }
    }

    public Id getId() {
        return id;
    }

    public FeatureFlagEntity getFlag() {
        return flag;
    }

    public void setFlag(FeatureFlagEntity flag) {
        this.flag = flag;
        if (id == null) {
            id = new Id();
        }
        id.setFlagId(flag.getId());
    }

    public String getEnvironment() {
        return id != null ? id.getEnvironment() : null;
    }

    public void setEnvironment(String environment) {
        if (id == null) {
            id = new Id();
        }
        id.setEnvironment(environment);
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public Long getLatestVersionId() {
        return latestVersionId;
    }

    public void setLatestVersionId(Long latestVersionId) {
        this.latestVersionId = latestVersionId;
    }

    public boolean isDraftDirty() {
        return draftDirty;
    }

    public void setDraftDirty(boolean draftDirty) {
        this.draftDirty = draftDirty;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
