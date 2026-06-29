package com.fms.repository;

import com.fms.domain.PublishJobEntity;
import com.fms.domain.enums.PublishJobStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PublishJobRepository extends JpaRepository<PublishJobEntity, Long>, PublishJobRepositoryCustom {

    @Query("""
            SELECT p FROM PublishJobEntity p
            JOIN FETCH p.flag f
            JOIN FETCH f.application
            WHERE p.id IN :ids
            ORDER BY p.createdAt ASC
            """)
    List<PublishJobEntity> findAllByIdInWithFlag(@Param("ids") Collection<Long> ids);

    boolean existsByFlag_IdAndEnvironmentAndStatusIn(
            java.util.UUID flagId, String environment, Collection<PublishJobStatus> statuses);

    boolean existsByFlag_IdAndEnvironmentAndStatus(
            java.util.UUID flagId, String environment, PublishJobStatus status);

    long countByStatus(PublishJobStatus status);

    List<PublishJobEntity> findByStatusAndEnvironmentOrderByCreatedAtAsc(
            PublishJobStatus status, String environment, Pageable pageable);
}
