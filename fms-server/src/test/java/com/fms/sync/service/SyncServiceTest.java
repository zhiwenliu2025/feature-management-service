package com.fms.sync.service;

import com.fms.cache.SnapshotCacheService;
import com.fms.common.exception.FmsErrorCode;
import com.fms.common.exception.FmsException;
import com.fms.config.FmsSyncProperties;
import com.fms.observability.FmsMetrics;
import com.fms.sync.dto.SnapshotResponse;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyncServiceTest {

    private static final String ENV = "dev";
    private static final String APP_ID = "checkout-service";

    @Mock
    private SnapshotCacheService snapshotCacheService;

    @Mock
    private SnapshotLoaderService snapshotLoaderService;

    private SyncService syncService;

    @BeforeEach
    void setUp() {
        FmsSyncProperties properties = new FmsSyncProperties();
        properties.setDeltaMaxGap(5);
        syncService = new SyncService(
                snapshotCacheService,
                snapshotLoaderService,
                properties,
                new FmsMetrics(new SimpleMeterRegistry()));
    }

    @Test
    void buildEtagIncludesAppIdWhenPresent() {
        assertEquals("\"dev:checkout-service:42\"", syncService.buildEtag(ENV, APP_ID, 42));
    }

    @Test
    void buildEtagOmitsAppIdWhenAbsent() {
        assertEquals("\"dev:42\"", syncService.buildEtag(ENV, null, 42));
        assertEquals("\"dev:42\"", syncService.buildEtag(ENV, "  ", 42));
    }

    @Test
    void getSnapshotReturnsNotModifiedWhenSinceVersionEqualsCurrent() {
        when(snapshotLoaderService.resolveCurrentVersion(ENV, APP_ID)).thenReturn(10L);

        SyncService.SnapshotResult result = syncService.getSnapshot(ENV, APP_ID, 10L, null);

        assertTrue(result.notModified());
        assertEquals(10L, result.configVersion());
        assertEquals("\"dev:checkout-service:10\"", result.etag());
    }

    @Test
    void getSnapshotReturnsNotModifiedWhenIfNoneMatchMatches() {
        when(snapshotLoaderService.resolveCurrentVersion(ENV, APP_ID)).thenReturn(7L);

        SyncService.SnapshotResult result = syncService.getSnapshot(
                ENV, APP_ID, null, "\"dev:checkout-service:7\"");

        assertTrue(result.notModified());
    }

    @Test
    void getSnapshotReturnsNotModifiedWhenWeakEtagMatches() {
        when(snapshotLoaderService.resolveCurrentVersion(ENV, APP_ID)).thenReturn(7L);

        SyncService.SnapshotResult result = syncService.getSnapshot(
                ENV, APP_ID, null, "W/\"dev:checkout-service:7\"");

        assertTrue(result.notModified());
    }

    @Test
    void getSnapshotThrowsWhenDeltaGapTooLarge() {
        when(snapshotLoaderService.resolveCurrentVersion(ENV, APP_ID)).thenReturn(10L);

        FmsException ex = assertThrows(FmsException.class,
                () -> syncService.getSnapshot(ENV, APP_ID, 1L, null));

        assertEquals(FmsErrorCode.DELTA_VERSION_GAP_TOO_LARGE, ex.errorCode());
    }

    @Test
    void getSnapshotReturnsCachedDeltaWhenAvailable() {
        SnapshotResponse delta = new SnapshotResponse(
                ENV, APP_ID, 5L, 4L, "delta", null, List.of(), List.of());

        when(snapshotLoaderService.resolveCurrentVersion(ENV, APP_ID)).thenReturn(5L);
        when(snapshotCacheService.getDelta(ENV, APP_ID, 4L, 5L)).thenReturn(Optional.of(delta));

        SyncService.SnapshotResult result = syncService.getSnapshot(ENV, APP_ID, 4L, null);

        assertEquals("delta", result.snapshot().syncType());
        assertEquals(5L, result.snapshot().configVersion());
    }

    @Test
    void getSnapshotLoadsFullSnapshotFromLoaderWhenCacheMisses() {
        SnapshotResponse full = new SnapshotResponse(
                ENV, APP_ID, 3L, null, "full", Instant.now(), List.of(), List.of());

        when(snapshotLoaderService.resolveCurrentVersion(ENV, APP_ID)).thenReturn(3L);
        when(snapshotCacheService.getSnapshot(ENV, APP_ID, 3L)).thenReturn(Optional.empty());
        when(snapshotLoaderService.loadFullSnapshot(ENV, APP_ID)).thenReturn(full);

        SyncService.SnapshotResult result = syncService.getSnapshot(ENV, APP_ID, null, null);

        assertEquals("full", result.snapshot().syncType());
        verify(snapshotLoaderService).loadFullSnapshot(ENV, APP_ID);
    }

    @Test
    void getSnapshotReturnsEmptyFullSnapshotWhenNoVersionExists() {
        when(snapshotLoaderService.resolveCurrentVersion(ENV, APP_ID)).thenReturn(0L);

        SyncService.SnapshotResult result = syncService.getSnapshot(ENV, APP_ID, null, null);

        assertEquals(0L, result.snapshot().configVersion());
        assertEquals("full", result.snapshot().syncType());
        assertTrue(result.snapshot().flags().isEmpty());
    }

    @Test
    void getCurrentVersionUsesEnvironmentPointerWhenAppIdMissing() {
        when(snapshotCacheService.getEnvironmentVersion(ENV)).thenReturn(12L);

        assertEquals(12L, syncService.getCurrentVersion(ENV, null));
    }

    @Test
    void openEventStreamDelegatesToCacheService() {
        SseEmitter emitter = new SseEmitter();
        when(snapshotCacheService.registerSseListener(ENV, APP_ID)).thenReturn(emitter);

        assertEquals(emitter, syncService.openEventStream(ENV, APP_ID, null));
        verify(snapshotCacheService).registerSseListener(ENV, APP_ID);
    }
}
