package com.fms.repository;

import com.fms.domain.ReleaseFlagEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReleaseFlagRepository extends JpaRepository<ReleaseFlagEntity, ReleaseFlagEntity.Id> {

    List<ReleaseFlagEntity> findByRelease_ReleaseId(String releaseId);
}
