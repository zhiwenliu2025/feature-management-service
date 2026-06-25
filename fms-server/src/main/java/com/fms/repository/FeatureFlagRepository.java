package com.fms.repository;

import com.fms.domain.FeatureFlagEntity;
import com.fms.domain.enums.FlagStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FeatureFlagRepository extends JpaRepository<FeatureFlagEntity, UUID> {

    Optional<FeatureFlagEntity> findByApplication_SlugAndKey(String applicationSlug, String key);

    boolean existsByApplication_SlugAndKey(String applicationSlug, String key);

    Page<FeatureFlagEntity> findByApplication_SlugAndStatus(String applicationSlug, FlagStatus status, Pageable pageable);

    Page<FeatureFlagEntity> findByApplication_Slug(String applicationSlug, Pageable pageable);
}
