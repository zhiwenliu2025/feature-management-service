package com.fms.repository;

import com.fms.domain.ConfigVersionHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConfigVersionHistoryRepository extends JpaRepository<ConfigVersionHistoryEntity, Long> {

    List<ConfigVersionHistoryEntity> findByEnvironmentAndConfigVersionGreaterThanAndConfigVersionLessThanEqualOrderByConfigVersionAsc(
            String environment, long sinceVersion, long currentVersion);

    Optional<ConfigVersionHistoryEntity> findByEnvironmentAndConfigVersion(String environment, long configVersion);
}
