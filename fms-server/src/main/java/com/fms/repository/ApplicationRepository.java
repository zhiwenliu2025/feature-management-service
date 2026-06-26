package com.fms.repository;

import com.fms.domain.ApplicationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<ApplicationEntity, UUID> {

    Optional<ApplicationEntity> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Page<ApplicationEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
