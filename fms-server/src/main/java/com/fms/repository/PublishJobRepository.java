package com.fms.repository;

import com.fms.domain.PublishJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublishJobRepository extends JpaRepository<PublishJobEntity, Long> {
}
