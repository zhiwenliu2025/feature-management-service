package com.fms.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fms.config.FmsSyncProperties;
import com.fms.sync.SseStreamManager;
import com.fms.sync.dto.SnapshotResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SnapshotCacheService {

    private final StringRedisTemplate redisTemplate;
    private final SnapshotCodec snapshotCodec;
    private final ObjectMapper objectMapper;
    private final FmsSyncProperties syncProperties;
    private final SseStreamManager sseStreamManager;

    public SnapshotCacheService(
            StringRedisTemplate redisTemplate,
            SnapshotCodec snapshotCodec,
            ObjectMapper objectMapper,
            FmsSyncProperties syncProperties,
            SseStreamManager sseStreamManager) {
        this.redisTemplate = redisTemplate;
        this.snapshotCodec = snapshotCodec;
        this.objectMapper = objectMapper;
        this.syncProperties = syncProperties;
        this.sseStreamManager = sseStreamManager;
    }

    public long getCurrentVersion(String environment, String appId) {
        String value = redisTemplate.opsForValue().get(RedisKeys.appVersionPointer(environment, appId));
        if (value == null) {
            return 0L;
        }
        return Long.parseLong(value);
    }

    public long getEnvironmentVersion(String environment) {
        String value = redisTemplate.opsForValue().get(RedisKeys.environmentVersion(environment));
        if (value == null) {
            return 0L;
        }
        return Long.parseLong(value);
    }

    public Optional<SnapshotResponse> getSnapshot(String environment, String appId, long version) {
        String encoded = redisTemplate.opsForValue().get(RedisKeys.appSnapshot(environment, appId, version));
        if (encoded == null) {
            return Optional.empty();
        }
        return Optional.of(toSnapshotResponse(snapshotCodec.decode(encoded)));
    }

    public Optional<SnapshotResponse> getDelta(String environment, String appId, long fromVersion, long toVersion) {
        String encoded = redisTemplate.opsForValue().get(RedisKeys.appDelta(environment, appId, fromVersion, toVersion));
        if (encoded == null) {
            return Optional.empty();
        }
        return Optional.of(toSnapshotResponse(snapshotCodec.decode(encoded)));
    }

    public void storeSnapshot(SnapshotResponse snapshot) {
        Duration ttl = syncProperties.snapshotTtl();
        String environment = snapshot.environment();
        String appId = snapshot.appId();
        long version = snapshot.configVersion();

        Map<String, Object> payload = toPayload(snapshot);
        redisTemplate.opsForValue().set(
                RedisKeys.appSnapshot(environment, appId, version),
                snapshotCodec.encode(payload),
                ttl);

        redisTemplate.opsForValue().set(
                RedisKeys.appVersionPointer(environment, appId),
                String.valueOf(version));

        redisTemplate.opsForValue().set(
                RedisKeys.environmentVersion(environment),
                String.valueOf(version));
    }

    public void storeDelta(SnapshotResponse delta) {
        if (delta.previousVersion() == null) {
            return;
        }
        Duration ttl = syncProperties.snapshotTtl();
        Map<String, Object> payload = toPayload(delta);
        redisTemplate.opsForValue().set(
                RedisKeys.appDelta(
                        delta.environment(),
                        delta.appId(),
                        delta.previousVersion(),
                        delta.configVersion()),
                snapshotCodec.encode(payload),
                ttl);
    }

    public void publishVersionChange(
            String environment,
            String appId,
            long configVersion,
            long previousVersion) {
        sseStreamManager.publishVersionEvent(environment, appId, configVersion, previousVersion);

        Map<String, Object> signal = new LinkedHashMap<>();
        signal.put("configVersion", configVersion);
        signal.put("previousVersion", previousVersion);
        signal.put("environment", environment);
        signal.put("changedApps", List.of(appId));
        signal.put("publishedAt", Instant.now().toString());

        try {
            redisTemplate.convertAndSend(
                    RedisKeys.pubSubChannel(environment),
                    objectMapper.writeValueAsString(signal));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to publish version change signal", ex);
        }
    }

    public SseEmitter registerSseListener(String environment, String appId) {
        return sseStreamManager.register(environment, appId);
    }

    public boolean isSnapshotCached(String environment, String appId, long version) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(RedisKeys.appSnapshot(environment, appId, version)));
    }

    private SnapshotResponse toSnapshotResponse(Map<String, Object> payload) {
        String environment = (String) payload.get("environment");
        String appId = (String) payload.get("appId");
        long configVersion = asLong(payload.get("configVersion"));
        Long previousVersion = payload.containsKey("previousVersion")
                ? asLong(payload.get("previousVersion"))
                : null;
        String syncType = (String) payload.getOrDefault("syncType", "full");
        Instant generatedAt = payload.containsKey("generatedAt")
                ? Instant.parse((String) payload.get("generatedAt"))
                : null;

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawFlags = (List<Map<String, Object>>) payload.getOrDefault("flags", List.of());
        List<SnapshotResponse.FlagSnapshot> flags = rawFlags.stream().map(this::toFlagSnapshot).toList();

        @SuppressWarnings("unchecked")
        List<String> deletedFlagKeys = (List<String>) payload.getOrDefault("deletedFlagKeys", List.of());

        return new SnapshotResponse(
                environment,
                appId,
                configVersion,
                previousVersion,
                syncType,
                generatedAt,
                flags,
                deletedFlagKeys);
    }

    private SnapshotResponse.FlagSnapshot toFlagSnapshot(Map<String, Object> map) {
        @SuppressWarnings("unchecked")
        List<Object> rules = (List<Object>) map.getOrDefault("rules", List.of());
        return new SnapshotResponse.FlagSnapshot(
                (String) map.get("key"),
                (String) map.get("type"),
                map.get("defaultValue"),
                (String) map.get("status"),
                (String) map.get("rolloutSalt"),
                rules,
                (String) map.get("releaseId"));
    }

    private Map<String, Object> toPayload(SnapshotResponse snapshot) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("environment", snapshot.environment());
        payload.put("appId", snapshot.appId());
        payload.put("configVersion", snapshot.configVersion());
        if (snapshot.previousVersion() != null) {
            payload.put("previousVersion", snapshot.previousVersion());
        }
        payload.put("syncType", snapshot.syncType());
        if (snapshot.generatedAt() != null) {
            payload.put("generatedAt", snapshot.generatedAt().toString());
        }
        payload.put("flags", snapshot.flags().stream().map(this::flagToMap).toList());
        payload.put("deletedFlagKeys", new ArrayList<>(snapshot.deletedFlagKeys()));
        return payload;
    }

    private Map<String, Object> flagToMap(SnapshotResponse.FlagSnapshot flag) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("key", flag.key());
        map.put("type", flag.type());
        map.put("defaultValue", flag.defaultValue());
        map.put("status", flag.status());
        map.put("rolloutSalt", flag.rolloutSalt());
        map.put("rules", flag.rules());
        if (flag.releaseId() != null) {
            map.put("releaseId", flag.releaseId());
        }
        return map;
    }

    private static long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}
