package com.fms.repository;

import com.fms.domain.KillSwitchOverrideEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface KillSwitchOverrideRepository extends JpaRepository<KillSwitchOverrideEntity, UUID> {

    List<KillSwitchOverrideEntity> findByFlag_IdAndEnvironmentAndActiveTrue(UUID flagId, String environment);
}
