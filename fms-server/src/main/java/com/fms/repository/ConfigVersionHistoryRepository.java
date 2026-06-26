package com.fms.repository;

import com.fms.domain.ConfigVersionHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfigVersionHistoryRepository extends JpaRepository<ConfigVersionHistoryEntity, Long> {
}
