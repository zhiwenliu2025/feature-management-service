package com.fms.sync.service;

import com.fms.cache.SnapshotCacheService;
import com.fms.domain.ConfigVersionHistoryEntity;
import com.fms.domain.FlagVersionEntity;
import com.fms.repository.ConfigVersionHistoryRepository;
import com.fms.repository.EnvironmentConfigRepository;
import com.fms.repository.FlagVersionRepository;
import com.fms.sync.dto.SnapshotResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class SnapshotLoaderService {

    private final FlagVersionRepository flagVersionRepository;
    private final ConfigVersionHistoryRepository configVersionHistoryRepository;
    private final EnvironmentConfigRepository environmentConfigRepository;
    private final SnapshotCacheService snapshotCacheService;

    public SnapshotLoaderService(
            FlagVersionRepository flagVersionRepository,
            ConfigVersionHistoryRepository configVersionHistoryRepository,
            EnvironmentConfigRepository environmentConfigRepository,
            SnapshotCacheService snapshotCacheService) {
        this.flagVersionRepository = flagVersionRepository;
        this.configVersionHistoryRepository = configVersionHistoryRepository;
        this.environmentConfigRepository = environmentConfigRepository;
        this.snapshotCacheService = snapshotCacheService;
    }

    @Transactional(readOnly = true)
    public SnapshotResponse compileFullSnapshot(String environment, String appId, long configVersion) {
        List<FlagVersionEntity> versions = flagVersionRepository.findCurrentPublishedVersions(appId, environment);
        List<SnapshotResponse.FlagSnapshot> flags = versions.stream()
                .map(this::toFlagSnapshot)
                .toList();

        return new SnapshotResponse(
                environment,
                appId,
                configVersion,
                null,
                "full",
                Instant.now(),
                flags,
                List.of());
    }

    @Transactional(readOnly = true)
    public SnapshotResponse loadFullSnapshot(String environment, String appId) {
        long configVersion = resolveCurrentVersion(environment, appId);
        SnapshotResponse snapshot = compileFullSnapshot(environment, appId, configVersion);
        if (configVersion > 0) {
            snapshotCacheService.storeSnapshot(snapshot);
        }
        return snapshot;
    }

    @Transactional(readOnly = true)
    public SnapshotResponse loadDeltaSnapshot(String environment, String appId, long sinceVersion, long currentVersion) {
        List<ConfigVersionHistoryEntity> history = configVersionHistoryRepository
                .findByEnvironmentAndConfigVersionGreaterThanAndConfigVersionLessThanEqualOrderByConfigVersionAsc(
                        environment, sinceVersion, currentVersion);

        Map<String, SnapshotResponse.FlagSnapshot> changedFlags = new LinkedHashMap<>();
        Set<String> deletedKeys = new LinkedHashSet<>();

        for (ConfigVersionHistoryEntity entry : history) {
            if (entry.getDeletedFlagKeys() != null) {
                deletedKeys.addAll(Arrays.asList(entry.getDeletedFlagKeys()));
            }
            List<UUID> changedFlagIds = entry.getChangedFlagIds() == null
                    ? List.of()
                    : Arrays.asList(entry.getChangedFlagIds());
            if (changedFlagIds.isEmpty()) {
                continue;
            }
            List<FlagVersionEntity> versions = flagVersionRepository.findSnapshotsAtConfigVersion(
                    appId, environment, entry.getConfigVersion(), changedFlagIds);
            for (FlagVersionEntity version : versions) {
                SnapshotResponse.FlagSnapshot flagSnapshot = toFlagSnapshot(version);
                changedFlags.put(flagSnapshot.key(), flagSnapshot);
                deletedKeys.remove(flagSnapshot.key());
            }
        }

        SnapshotResponse delta = new SnapshotResponse(
                environment,
                appId,
                currentVersion,
                sinceVersion,
                "delta",
                null,
                new ArrayList<>(changedFlags.values()),
                new ArrayList<>(deletedKeys));

        snapshotCacheService.storeDelta(delta);
        return delta;
    }

    public long resolveCurrentVersion(String environment, String appId) {
        if (appId != null && !appId.isBlank()) {
            long cachedVersion = snapshotCacheService.getCurrentVersion(environment, appId);
            if (cachedVersion > 0) {
                return cachedVersion;
            }
        } else {
            long envVersion = snapshotCacheService.getEnvironmentVersion(environment);
            if (envVersion > 0) {
                return envVersion;
            }
        }
        return environmentConfigRepository.findById(environment)
                .map(config -> config.getCurrentConfigVersion())
                .orElse(0L);
    }

    @SuppressWarnings("unchecked")
    private SnapshotResponse.FlagSnapshot toFlagSnapshot(FlagVersionEntity version) {
        Object snapshot = version.getSnapshot();
        if (!(snapshot instanceof Map<?, ?> map)) {
            throw new IllegalStateException("Flag snapshot payload is invalid for flag version " + version.getId());
        }
        Map<String, Object> payload = (Map<String, Object>) map;
        List<Object> rules = payload.get("rules") instanceof List<?> list
                ? new ArrayList<>(list)
                : List.of();
        return new SnapshotResponse.FlagSnapshot(
                (String) payload.get("key"),
                (String) payload.get("type"),
                payload.get("defaultValue"),
                (String) payload.get("status"),
                (String) payload.get("rolloutSalt"),
                rules,
                (String) payload.get("releaseId"));
    }
}
