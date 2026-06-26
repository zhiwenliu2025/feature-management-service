package com.fms.sync.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SnapshotResponse(
        String environment,
        String appId,
        long configVersion,
        Long previousVersion,
        String syncType,
        Instant generatedAt,
        List<FlagSnapshot> flags,
        List<String> deletedFlagKeys
) {
    public String etag() {
        return "\"" + environment + ":" + appId + ":" + configVersion + "\"";
    }

    public record FlagSnapshot(
            String key,
            String type,
            Object defaultValue,
            String status,
            String rolloutSalt,
            List<Object> rules,
            String releaseId
    ) {
    }
}
