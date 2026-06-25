package com.fms.sync;

import com.fms.sync.dto.SnapshotResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/v1/sync")
@Tag(name = "Config Sync", description = "SDK configuration sync and SSE streaming")
public class SyncController {

    private final SyncService syncService;

    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    @GetMapping("/snapshot")
    @Operation(summary = "Get full or incremental configuration snapshot")
    ResponseEntity<SnapshotResponse> getSnapshot(
            @RequestParam String environment,
            @RequestParam String appId,
            @RequestParam(required = false) Long sinceVersion) {
        SnapshotResponse snapshot = syncService.getSnapshot(environment, appId, sinceVersion);
        return ResponseEntity.ok()
                .header(HttpHeaders.ETAG, snapshot.etag())
                .header("X-Config-Version", String.valueOf(snapshot.configVersion()))
                .body(snapshot);
    }

    @GetMapping("/version")
    @Operation(summary = "Lightweight version check")
    ResponseEntity<Void> getVersion(
            @RequestParam String environment,
            @RequestParam(required = false) String appId) {
        long version = syncService.getCurrentVersion(environment, appId);
        return ResponseEntity.ok()
                .header("X-Config-Version", String.valueOf(version))
                .header(HttpHeaders.ETAG, "\"" + environment + ":" + appId + ":" + version + "\"")
                .build();
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE stream of version updates")
    SseEmitter stream(
            @RequestParam String environment,
            @RequestParam String appId,
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {
        return syncService.openEventStream(environment, appId, lastEventId);
    }
}
