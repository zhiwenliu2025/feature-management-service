package com.fms.sync;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fms.config.FmsSyncProperties;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SseStreamManager {

    private static final Logger log = LoggerFactory.getLogger(SseStreamManager.class);

    private final FmsSyncProperties syncProperties;
    private final ObjectMapper objectMapper;
    private final Map<String, CopyOnWriteArrayList<SseEmitter>> listeners = new ConcurrentHashMap<>();

    public SseStreamManager(FmsSyncProperties syncProperties, ObjectMapper objectMapper) {
        this.syncProperties = syncProperties;
        this.objectMapper = objectMapper;
    }

    public SseEmitter register(String environment, String appId) {
        String key = listenerKey(environment, appId);
        SseEmitter emitter = new SseEmitter(syncProperties.sseTimeout().toMillis());
        listeners.computeIfAbsent(key, ignored -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(key, emitter));
        emitter.onTimeout(() -> removeEmitter(key, emitter));
        emitter.onError(ex -> removeEmitter(key, emitter));

        return emitter;
    }

    public void publishVersionEvent(
            String environment,
            String appId,
            long configVersion,
            long previousVersion) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(Map.of(
                    "configVersion", configVersion,
                    "previousVersion", previousVersion,
                    "environment", environment,
                    "appId", appId));
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize SSE version event", ex);
            return;
        }
        broadcast(listenerKey(environment, appId), "version", payload);
    }

    public void handlePubSubMessage(String environment, String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String eventEnvironment = node.path("environment").asText(environment);
            long configVersion = node.path("configVersion").asLong();
            long previousVersion = node.path("previousVersion").asLong();
            JsonNode changedApps = node.path("changedApps");
            if (!changedApps.isArray()) {
                return;
            }
            for (JsonNode appNode : changedApps) {
                publishVersionEvent(eventEnvironment, appNode.asText(), configVersion, previousVersion);
            }
        } catch (IOException ex) {
            log.warn("Failed to process pub/sub message for environment {}", environment, ex);
        }
    }

    @Scheduled(fixedDelayString = "${fms.sync.sse-heartbeat-interval-ms:30000}")
    void sendHeartbeats() {
        String payload = "{\"timestamp\":\"" + Instant.now() + "\"}";
        listeners.forEach((key, emitters) -> broadcast(key, "heartbeat", payload));
    }

    @PreDestroy
    void shutdown() {
        listeners.values().forEach(emitters -> emitters.forEach(SseEmitter::complete));
        listeners.clear();
    }

    private void broadcast(String key, String eventName, String payload) {
        List<SseEmitter> emitters = listeners.get(key);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(payload));
            } catch (IOException ex) {
                removeEmitter(key, emitter);
                emitter.completeWithError(ex);
            }
        }
    }

    private void removeEmitter(String key, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = listeners.get(key);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            listeners.remove(key, emitters);
        }
    }

    private static String listenerKey(String environment, String appId) {
        return environment + ":" + appId;
    }
}
