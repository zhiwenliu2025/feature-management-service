package com.fms.repository;

import com.fms.domain.ApiKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApiKeyRepository extends JpaRepository<ApiKeyEntity, UUID> {

    List<ApiKeyEntity> findByApplication_SlugOrderByCreatedAtDesc(String applicationSlug);
}
