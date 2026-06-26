package com.fms.domain;

import com.fms.domain.enums.KillSwitchScope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "kill_switch_overrides")
public class KillSwitchOverrideEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flag_id", nullable = false)
    private FeatureFlagEntity flag;

    @Column(nullable = false, length = 32)
    private String environment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private KillSwitchScope scope;

    @Column(name = "region_code", length = 8)
    private String regionCode;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "forced_value", nullable = false, columnDefinition = "jsonb")
    private Object forcedValue;

    @Column(name = "activated_by", nullable = false, length = 256)
    private String activatedBy;

    @Column(name = "activated_at", nullable = false)
    private Instant activatedAt = Instant.now();

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    @Column(name = "deactivated_by", length = 256)
    private String deactivatedBy;

    public UUID getId() {
        return id;
    }

    public FeatureFlagEntity getFlag() {
        return flag;
    }

    public void setFlag(FeatureFlagEntity flag) {
        this.flag = flag;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public KillSwitchScope getScope() {
        return scope;
    }

    public void setScope(KillSwitchScope scope) {
        this.scope = scope;
    }

    public String getRegionCode() {
        return regionCode;
    }

    public void setRegionCode(String regionCode) {
        this.regionCode = regionCode;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Object getForcedValue() {
        return forcedValue;
    }

    public void setForcedValue(Object forcedValue) {
        this.forcedValue = forcedValue;
    }

    public String getActivatedBy() {
        return activatedBy;
    }

    public void setActivatedBy(String activatedBy) {
        this.activatedBy = activatedBy;
    }

    public Instant getActivatedAt() {
        return activatedAt;
    }

    public Instant getDeactivatedAt() {
        return deactivatedAt;
    }

    public void setDeactivatedAt(Instant deactivatedAt) {
        this.deactivatedAt = deactivatedAt;
    }

    public String getDeactivatedBy() {
        return deactivatedBy;
    }

    public void setDeactivatedBy(String deactivatedBy) {
        this.deactivatedBy = deactivatedBy;
    }
}
