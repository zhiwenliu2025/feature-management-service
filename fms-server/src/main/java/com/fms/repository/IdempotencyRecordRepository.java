package com.fms.repository;

import com.fms.domain.IdempotencyRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecordEntity, UUID> {

    Optional<IdempotencyRecordEntity> findByIdempotencyKeyAndOperationKeyAndExpiresAtAfter(
            String idempotencyKey, String operationKey, Instant now);

    void deleteByExpiresAtBefore(Instant cutoff);
}
