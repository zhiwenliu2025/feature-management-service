package com.fms.repository;

import com.fms.domain.FlagVersionEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FlagVersionRepository extends JpaRepository<FlagVersionEntity, Long> {

    List<FlagVersionEntity> findByFlag_IdAndEnvironmentOrderByFlagVersionDesc(UUID flagId, String environment, Pageable pageable);

    Optional<FlagVersionEntity> findByFlag_IdAndEnvironmentAndFlagVersion(UUID flagId, String environment, int flagVersion);

    Optional<FlagVersionEntity> findByFlag_Application_SlugAndFlag_KeyAndEnvironmentAndFlagVersion(
            String appId, String flagKey, String environment, int flagVersion);

    int countByFlag_IdAndEnvironment(UUID flagId, String environment);

    @Query(value = """
            SELECT fv.* FROM flag_versions fv
            JOIN flag_environment_state fes ON fes.latest_version_id = fv.id
            JOIN feature_flags f ON f.id = fes.flag_id
            JOIN applications a ON a.id = f.application_id
            WHERE a.slug = :appId
              AND fes.environment = :environment
              AND fes.is_published = true
              AND f.status <> 'archived'
            """, nativeQuery = true)
    List<FlagVersionEntity> findCurrentPublishedVersions(
            @Param("appId") String appId,
            @Param("environment") String environment);

    @Query("""
            SELECT fv FROM FlagVersionEntity fv
            JOIN fv.flag f
            JOIN f.application a
            WHERE a.slug = :appId
              AND fv.environment = :environment
              AND fv.configVersion = :configVersion
              AND f.id IN :flagIds
            """)
    List<FlagVersionEntity> findSnapshotsAtConfigVersion(
            @Param("appId") String appId,
            @Param("environment") String environment,
            @Param("configVersion") long configVersion,
            @Param("flagIds") List<UUID> flagIds);

    Optional<FlagVersionEntity> findTopByFlag_Application_SlugAndFlag_KeyAndEnvironmentAndConfigVersionOrderByFlagVersionDesc(
            String appId, String flagKey, String environment, long configVersion);
}
