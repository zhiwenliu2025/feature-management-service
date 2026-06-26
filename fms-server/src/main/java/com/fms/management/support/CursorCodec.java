package com.fms.management.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

public final class CursorCodec {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CursorCodec() {
    }

    public record Cursor(Instant createdAt, UUID id) {
    }

    public static String encode(Instant createdAt, UUID id) {
        try {
            byte[] json = MAPPER.writeValueAsBytes(new CursorPayload(createdAt.toString(), id.toString()));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to encode cursor", e);
        }
    }

    public static Cursor decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(cursor.getBytes(StandardCharsets.UTF_8));
            CursorPayload payload = MAPPER.readValue(decoded, CursorPayload.class);
            return new Cursor(Instant.parse(payload.createdAt()), UUID.fromString(payload.id()));
        } catch (Exception e) {
            return null;
        }
    }

    private record CursorPayload(String createdAt, String id) {
    }
}
