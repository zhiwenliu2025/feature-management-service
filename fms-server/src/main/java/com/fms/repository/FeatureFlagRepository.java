package com.fms.repository;

import com.fms.domain.FeatureFlagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeatureFlagRepository extends JpaRepository<FeatureFlagEntity, UUID> {

    Optional<FeatureFlagEntity> findByApplication_SlugAndKey(String applicationSlug, String key);

    boolean existsByApplication_SlugAndKey(String applicationSlug, String key);

    @Query(value = """
            SELECT f.* FROM feature_flags f
            JOIN applications a ON a.id = f.application_id
            WHERE a.slug = :appId
              AND (:status IS NULL OR f.status = :status)
              AND (:search IS NULL OR lower(f.name) LIKE lower('%' || :search || '%')
                   OR lower(f.key) LIKE lower('%' || :search || '%'))
              AND (:tag IS NULL OR EXISTS (
                    SELECT 1 FROM feature_flag_tags fft
                    JOIN tags t ON t.id = fft.tag_id
                    WHERE fft.flag_id = f.id AND t.name = :tag
              ))
              AND (
                :cursorCreatedAt IS NULL
                OR (f.created_at, f.id) < (:cursorCreatedAt::timestamptz, :cursorId::uuid)
              )
            ORDER BY f.created_at DESC, f.id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<FeatureFlagEntity> searchFlags(
            @Param("appId") String appId,
            @Param("status") String status,
            @Param("search") String search,
            @Param("tag") String tag,
            @Param("cursorCreatedAt") java.time.Instant cursorCreatedAt,
            @Param("cursorId") String cursorId,
            @Param("limit") int limit);

    @Query(value = """
            SELECT COUNT(*) FROM feature_flags f
            JOIN applications a ON a.id = f.application_id
            WHERE a.slug = :appId
              AND (:status IS NULL OR f.status = :status)
              AND (:search IS NULL OR lower(f.name) LIKE lower('%' || :search || '%')
                   OR lower(f.key) LIKE lower('%' || :search || '%'))
              AND (:tag IS NULL OR EXISTS (
                    SELECT 1 FROM feature_flag_tags fft
                    JOIN tags t ON t.id = fft.tag_id
                    WHERE fft.flag_id = f.id AND t.name = :tag
              ))
            """, nativeQuery = true)
    long countSearchFlags(
            @Param("appId") String appId,
            @Param("status") String status,
            @Param("search") String search,
            @Param("tag") String tag);

    @Query(value = """
            SELECT t.name FROM tags t
            JOIN feature_flag_tags fft ON fft.tag_id = t.id
            WHERE fft.flag_id = :flagId
            ORDER BY t.name
            """, nativeQuery = true)
    List<String> findTagNamesByFlagId(@Param("flagId") UUID flagId);

    @Modifying
    @Query(value = """
            INSERT INTO feature_flag_tags (flag_id, tag_id, created_at)
            SELECT :flagId, t.id, now() FROM tags t WHERE t.name = :tagName
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    void linkTag(@Param("flagId") UUID flagId, @Param("tagName") String tagName);
}
