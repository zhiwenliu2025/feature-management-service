package com.fms.sync.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fms.sync.dto.SnapshotResponse;
import com.fms.sync.service.SyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

@RestController
@RequestMapping("/v1/sync")
@Tag(name = "Config Sync", description = "SDK configuration sync and SSE streaming")
public class SyncController {

    private final SyncService syncService;
    private final ObjectMapper objectMapper;

    public SyncController(SyncService syncService, ObjectMapper objectMapper) {
        this.syncService = syncService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/snapshot")
    @Operation(summary = "Get full or incremental configuration snapshot")
    ResponseEntity<?> getSnapshot(
            @RequestParam String environment,
            @RequestParam String appId,
            @RequestParam(required = false) Long sinceVersion,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch,
            @RequestHeader(value = HttpHeaders.ACCEPT_ENCODING, required = false) String acceptEncoding)
            throws IOException {
        SyncService.SnapshotResult result = syncService.getSnapshot(environment, appId, sinceVersion, ifNoneMatch);
        if (result.notModified()) {
            return ResponseEntity.status(304)
                    .header(HttpHeaders.ETAG, result.etag())
                    .header("X-Config-Version", String.valueOf(result.configVersion()))
                    .build();
        }

        SnapshotResponse snapshot = result.snapshot();
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .header(HttpHeaders.ETAG, result.etag())
                .header("X-Config-Version", String.valueOf(snapshot.configVersion()));

        if (acceptEncoding != null && acceptEncoding.contains("gzip")) {
            byte[] json = objectMapper.writeValueAsBytes(snapshot);
            byte[] compressed = gzip(json);
            return builder
                    .header(HttpHeaders.CONTENT_ENCODING, "gzip")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(compressed);
        }

        return builder.body(snapshot);
    }

    @RequestMapping(method = RequestMethod.HEAD, path = "/version")
    @Operation(summary = "Lightweight version check")
    ResponseEntity<Void> headVersion(
            @RequestParam String environment,
            @RequestParam(required = false) String appId) {
        long version = syncService.getCurrentVersion(environment, appId);
        return ResponseEntity.ok()
                .header("X-Config-Version", String.valueOf(version))
                .header(HttpHeaders.ETAG, syncService.buildEtag(environment, appId, version))
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

    private static byte[] gzip(byte[] input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
            gzip.write(input);
        }
        return output.toByteArray();
    }
}
