package com.fms.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public class PublishJobRepositoryImpl implements PublishJobRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Long> claimJobIds(String workerId, Instant now, int limit) {
        List<?> ids = entityManager.createNativeQuery("""
                        WITH claimable AS (
                            SELECT id FROM publish_jobs
                            WHERE status IN ('pending', 'failed')
                              AND attempt_count < max_attempts
                              AND next_retry_at <= :now
                            ORDER BY created_at
                            FOR UPDATE SKIP LOCKED
                            LIMIT :limit
                        )
                        UPDATE publish_jobs p
                        SET status = 'processing',
                            locked_by = :workerId,
                            locked_at = now(),
                            attempt_count = attempt_count + 1
                        FROM claimable c
                        WHERE p.id = c.id
                        RETURNING p.id
                        """)
                .setParameter("now", now)
                .setParameter("workerId", workerId)
                .setParameter("limit", limit)
                .getResultList();
        return ids.stream().map(id -> ((Number) id).longValue()).toList();
    }
}
