package com.fms.repository;

import com.fms.domain.ApiKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ApiKeyRepository extends JpaRepository<ApiKeyEntity, UUID> {

    List<ApiKeyEntity> findByApplication_SlugOrderByCreatedAtDesc(String applicationSlug);

    @Query("""
            SELECT k FROM ApiKeyEntity k
            JOIN FETCH k.application
            WHERE k.keyPrefix = :prefix
              AND k.revokedAt IS NULL
            """)
    List<ApiKeyEntity> findActiveByKeyPrefix(@Param("prefix") String prefix);
}
