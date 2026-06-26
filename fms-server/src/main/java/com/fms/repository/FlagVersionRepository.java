package com.fms.repository;

import com.fms.domain.FlagVersionEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FlagVersionRepository extends JpaRepository<FlagVersionEntity, Long> {

    List<FlagVersionEntity> findByFlag_IdAndEnvironmentOrderByFlagVersionDesc(UUID flagId, String environment, Pageable pageable);

    Optional<FlagVersionEntity> findByFlag_IdAndEnvironmentAndFlagVersion(UUID flagId, String environment, int flagVersion);

    Optional<FlagVersionEntity> findByFlag_Application_SlugAndFlag_KeyAndEnvironmentAndFlagVersion(
            String appId, String flagKey, String environment, int flagVersion);

    int countByFlag_IdAndEnvironment(UUID flagId, String environment);
}
