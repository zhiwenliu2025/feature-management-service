package com.fms.repository;

import com.fms.domain.EnvironmentConfigEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EnvironmentConfigRepository extends JpaRepository<EnvironmentConfigEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM EnvironmentConfigEntity e WHERE e.environment = :environment")
    Optional<EnvironmentConfigEntity> findByEnvironmentForUpdate(@Param("environment") String environment);
}
