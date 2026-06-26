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
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "release_flags")
public class ReleaseFlagEntity {

    @EmbeddedId
    private Id id = new Id();

    @MapsId("releaseId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "release_id", nullable = false)
    private ReleaseEntity release;

    @MapsId("flagId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flag_id", nullable = false)
    private FeatureFlagEntity flag;

    @Column(nullable = false, length = 32)
    private String environment;

    @Column(name = "config_version")
    private Long configVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    @Embeddable
    public static class Id implements Serializable {
        @Column(name = "release_id")
        private UUID releaseId;

        @Column(name = "flag_id")
        private UUID flagId;

        public Id() {
        }

        public Id(UUID releaseId, UUID flagId) {
            this.releaseId = releaseId;
            this.flagId = flagId;
        }

        public UUID getReleaseId() {
            return releaseId;
        }

        public void setReleaseId(UUID releaseId) {
            this.releaseId = releaseId;
        }

        public UUID getFlagId() {
            return flagId;
        }

        public void setFlagId(UUID flagId) {
            this.flagId = flagId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Id that)) {
                return false;
            }
            return Objects.equals(releaseId, that.releaseId) && Objects.equals(flagId, that.flagId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(releaseId, flagId);
        }
    }

    public ReleaseEntity getRelease() {
        return release;
    }

    public void setRelease(ReleaseEntity release) {
        this.release = release;
        if (id == null) {
            id = new Id();
        }
        id.setReleaseId(release.getId());
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
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public Long getConfigVersion() {
        return configVersion;
    }

    public void setConfigVersion(Long configVersion) {
        this.configVersion = configVersion;
    }
}
