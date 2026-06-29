package com.fms.sync.service;

import com.fms.cache.SnapshotCacheService;
import com.fms.common.exception.FmsErrorCode;
import com.fms.common.exception.FmsException;
import com.fms.config.FmsSyncProperties;
import com.fms.sync.dto.SnapshotResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Optional;

@Service
public class SyncService {

    private final SnapshotCacheService snapshotCacheService;
    private final SnapshotLoaderService snapshotLoaderService;
    private final FmsSyncProperties syncProperties;

    public SyncService(
            SnapshotCacheService snapshotCacheService,
            SnapshotLoaderService snapshotLoaderService,
            FmsSyncProperties syncProperties) {
        this.snapshotCacheService = snapshotCacheService;
        this.snapshotLoaderService = snapshotLoaderService;
        this.syncProperties = syncProperties;
    }

    public SnapshotResult getSnapshot(String environment, String appId, Long sinceVersion, String ifNoneMatch) {
        long currentVersion = snapshotLoaderService.resolveCurrentVersion(environment, appId);
        String etag = buildEtag(environment, appId, currentVersion);

        if (matchesEtag(ifNoneMatch, etag) || (sinceVersion != null && sinceVersion >= currentVersion)) {
            return SnapshotResult.notModified(currentVersion, etag);
        }

        if (sinceVersion == null || sinceVersion <= 0) {
            return SnapshotResult.ok(loadFullSnapshot(environment, appId, currentVersion), etag);
        }

        long versionGap = currentVersion - sinceVersion;
        if (versionGap > syncProperties.deltaMaxGap()) {
            throw new FmsException(
                    FmsErrorCode.DELTA_VERSION_GAP_TOO_LARGE,
                    "Requested sinceVersion is outside the retention window; request a full snapshot.");
        }

        Optional<SnapshotResponse> cachedDelta = snapshotCacheService.getDelta(
                environment, appId, sinceVersion, currentVersion);
        SnapshotResponse delta = cachedDelta.orElseGet(() -> snapshotLoaderService.loadDeltaSnapshot(
                environment, appId, sinceVersion, currentVersion));
        return SnapshotResult.ok(delta, buildEtag(environment, appId, delta.configVersion()));
    }

    public long getCurrentVersion(String environment, String appId) {
        if (appId == null || appId.isBlank()) {
            long envVersion = snapshotCacheService.getEnvironmentVersion(environment);
            if (envVersion > 0) {
                return envVersion;
            }
            return snapshotLoaderService.resolveCurrentVersion(environment, null);
        }
        return snapshotLoaderService.resolveCurrentVersion(environment, appId);
    }

    public String buildEtag(String environment, String appId, long version) {
        if (appId == null || appId.isBlank()) {
            return "\"" + environment + ":" + version + "\"";
        }
        return "\"" + environment + ":" + appId + ":" + version + "\"";
    }

    public SseEmitter openEventStream(String environment, String appId, String lastEventId) {
        return snapshotCacheService.registerSseListener(environment, appId);
    }

    private SnapshotResponse loadFullSnapshot(String environment, String appId, long currentVersion) {
        if (currentVersion <= 0) {
            return new SnapshotResponse(
                    environment,
                    appId,
                    0L,
                    null,
                    "full",
                    java.time.Instant.now(),
                    java.util.List.of(),
                    java.util.List.of());
        }
        return snapshotCacheService.getSnapshot(environment, appId, currentVersion)
                .orElseGet(() -> snapshotLoaderService.loadFullSnapshot(environment, appId));
    }

    private static boolean matchesEtag(String ifNoneMatch, String etag) {
        if (ifNoneMatch == null || ifNoneMatch.isBlank()) {
            return false;
        }
        String trimmed = ifNoneMatch.trim();
        if (trimmed.startsWith("W/")) {
            trimmed = trimmed.substring(2).trim();
        }
        return trimmed.equals(etag);
    }

    public record SnapshotResult(SnapshotResponse snapshot, long configVersion, String etag, boolean notModified) {

        static SnapshotResult ok(SnapshotResponse snapshot, String etag) {
            return new SnapshotResult(snapshot, snapshot.configVersion(), etag, false);
        }

        static SnapshotResult notModified(long configVersion, String etag) {
            return new SnapshotResult(null, configVersion, etag, true);
        }
    }
}
