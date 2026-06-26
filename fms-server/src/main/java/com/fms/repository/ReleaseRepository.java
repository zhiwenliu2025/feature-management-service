package com.fms.repository;

import com.fms.domain.ReleaseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReleaseRepository extends JpaRepository<ReleaseEntity, UUID> {

    Optional<ReleaseEntity> findByReleaseId(String releaseId);

    boolean existsByReleaseId(String releaseId);

    Page<ReleaseEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
