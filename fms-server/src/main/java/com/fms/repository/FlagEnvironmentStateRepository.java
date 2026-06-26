package com.fms.repository;

import com.fms.domain.FlagEnvironmentStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FlagEnvironmentStateRepository extends JpaRepository<FlagEnvironmentStateEntity, FlagEnvironmentStateEntity.Id> {

    List<FlagEnvironmentStateEntity> findByFlag_Id(UUID flagId);

    Optional<FlagEnvironmentStateEntity> findByFlag_IdAndId_Environment(UUID flagId, String environment);
}
