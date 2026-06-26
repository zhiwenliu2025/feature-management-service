package com.fms.repository;

import com.fms.domain.FlagRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FlagRuleRepository extends JpaRepository<FlagRuleEntity, UUID> {

    List<FlagRuleEntity> findByFlag_IdAndEnvironmentOrderByPriorityAsc(UUID flagId, String environment);

    void deleteByFlag_IdAndEnvironment(UUID flagId, String environment);

    long countByFlag_IdAndEnvironment(UUID flagId, String environment);
}
