package com.fms.repository;

import com.fms.domain.PublishJobEntity;
import com.fms.domain.enums.PublishJobStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PublishJobRepository extends JpaRepository<PublishJobEntity, Long> {

    @Query("""
            SELECT p FROM PublishJobEntity p
            JOIN FETCH p.flag f
            JOIN FETCH f.application
            WHERE p.status = :status
            ORDER BY p.createdAt ASC
            """)
    List<PublishJobEntity> findByStatusOrderByCreatedAtAsc(PublishJobStatus status, Pageable pageable);

    boolean existsByFlag_IdAndEnvironmentAndStatus(
            java.util.UUID flagId, String environment, PublishJobStatus status);
}
