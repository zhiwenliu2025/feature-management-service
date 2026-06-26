package com.fms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "environments")
public class EnvironmentEntity {

    @Id
    @Column(length = 32)
    private String name;

    @Column(name = "display_name", nullable = false, length = 64)
    private String displayName;

    @Column(name = "sort_order", nullable = false)
    private short sortOrder;

    @Column(name = "is_production", nullable = false)
    private boolean production;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public short getSortOrder() {
        return sortOrder;
    }

    public boolean isProduction() {
        return production;
    }
}
