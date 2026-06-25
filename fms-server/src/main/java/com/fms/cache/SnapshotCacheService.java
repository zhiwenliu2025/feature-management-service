package com.fms.cache;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SnapshotCacheService {

    private static final String VERSION_KEY_TEMPLATE = "{fms:%s:%s}:snap:current";

    private final StringRedisTemplate redisTemplate;
    private final Map<String, SseEmitter> sseEmitters = new ConcurrentHashMap<>();

    public SnapshotCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public long getCurrentVersion(String environment, String appId) {
        String key = VERSION_KEY_TEMPLATE.formatted(environment, appId);
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return 0L;
        }
        return Long.parseLong(value);
    }

    public void setCurrentVersion(String environment, String appId, long version) {
        String key = VERSION_KEY_TEMPLATE.formatted(environment, appId);
        redisTemplate.opsForValue().set(key, String.valueOf(version));
        notifyListeners(environment, appId, version);
    }

    public SseEmitter registerSseListener(String environment, String appId) {
        String listenerKey = environment + ":" + appId;
        SseEmitter emitter = new SseEmitter(Duration.ofHours(1).toMillis());
        sseEmitters.put(listenerKey, emitter);
        emitter.onCompletion(() -> sseEmitters.remove(listenerKey));
        emitter.onTimeout(() -> sseEmitters.remove(listenerKey));
        return emitter;
    }

    private void notifyListeners(String environment, String appId, long version) {
        String listenerKey = environment + ":" + appId;
        SseEmitter emitter = sseEmitters.get(listenerKey);
        if (emitter == null) {
            return;
        }
        try {
            emitter.send(SseEmitter.event()
                    .name("version")
                    .data("{\"configVersion\":" + version + ",\"environment\":\"" + environment
                            + "\",\"appId\":\"" + appId + "\"}"));
        } catch (IOException ex) {
            sseEmitters.remove(listenerKey);
            emitter.completeWithError(ex);
        }
    }
}
