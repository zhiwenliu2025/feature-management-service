package com.fms.sync;

import com.fms.cache.SnapshotCacheService;
import com.fms.sync.dto.SnapshotResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.List;

@Service
public class SyncService {

    private final SnapshotCacheService snapshotCacheService;

    public SyncService(SnapshotCacheService snapshotCacheService) {
        this.snapshotCacheService = snapshotCacheService;
    }

    public SnapshotResponse getSnapshot(String environment, String appId, Long sinceVersion) {
        long currentVersion = snapshotCacheService.getCurrentVersion(environment, appId);
        String syncType = sinceVersion == null || sinceVersion < currentVersion - 1 ? "full" : "delta";

        return new SnapshotResponse(
                environment,
                appId,
                currentVersion,
                sinceVersion,
                syncType,
                Instant.now(),
                List.of(),
                List.of());
    }

    public long getCurrentVersion(String environment, String appId) {
        return snapshotCacheService.getCurrentVersion(environment, appId);
    }

    public SseEmitter openEventStream(String environment, String appId, String lastEventId) {
        return snapshotCacheService.registerSseListener(environment, appId);
    }
}
