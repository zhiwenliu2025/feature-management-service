package com.fms.explain.service;

import com.fms.cache.SnapshotCacheService;
import com.fms.common.exception.FmsErrorCode;
import com.fms.common.exception.FmsException;
import com.fms.domain.FeatureFlagEntity;
import com.fms.domain.KillSwitchOverrideEntity;
import com.fms.domain.enums.KillSwitchScope;
import com.fms.explain.dto.ExplainRequest;
import com.fms.explain.dto.ExplainResponse;
import com.fms.explain.dto.ReplayExplainRequest;
import com.fms.repository.ConfigVersionHistoryRepository;
import com.fms.repository.FeatureFlagRepository;
import com.fms.repository.FlagVersionRepository;
import com.fms.repository.KillSwitchOverrideRepository;
import com.fms.repository.ReleaseRepository;
import com.fms.ruleengine.DefaultRuleEngine;
import com.fms.ruleengine.EvaluationContext;
import com.fms.ruleengine.ExplainResult;
import com.fms.ruleengine.ReasonCode;
import com.fms.ruleengine.RuleEngine;
import com.fms.ruleengine.TraceStep;
import com.fms.security.DataPlaneAuthzService;
import com.fms.sync.dto.SnapshotResponse;
import com.fms.sync.service.SnapshotLoaderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ExplainService {

    private static final String SCHEMA_VERSION = "1.0";

    private final RuleEngine ruleEngine;
    private final SnapshotCacheService snapshotCacheService;
    private final SnapshotLoaderService snapshotLoaderService;
    private final FeatureFlagRepository featureFlagRepository;
    private final KillSwitchOverrideRepository killSwitchOverrideRepository;
    private final ReleaseRepository releaseRepository;
    private final ConfigVersionHistoryRepository configVersionHistoryRepository;
    private final FlagVersionRepository flagVersionRepository;
    private final DataPlaneAuthzService dataPlaneAuthzService;

    public ExplainService(
            RuleEngine ruleEngine,
            SnapshotCacheService snapshotCacheService,
            SnapshotLoaderService snapshotLoaderService,
            FeatureFlagRepository featureFlagRepository,
            KillSwitchOverrideRepository killSwitchOverrideRepository,
            ReleaseRepository releaseRepository,
            ConfigVersionHistoryRepository configVersionHistoryRepository,
            FlagVersionRepository flagVersionRepository,
            DataPlaneAuthzService dataPlaneAuthzService) {
        this.ruleEngine = ruleEngine;
        this.snapshotCacheService = snapshotCacheService;
        this.snapshotLoaderService = snapshotLoaderService;
        this.featureFlagRepository = featureFlagRepository;
        this.killSwitchOverrideRepository = killSwitchOverrideRepository;
        this.releaseRepository = releaseRepository;
        this.configVersionHistoryRepository = configVersionHistoryRepository;
        this.flagVersionRepository = flagVersionRepository;
        this.dataPlaneAuthzService = dataPlaneAuthzService;
    }

    @Transactional(readOnly = true)
    public ExplainResponse explain(String flagKey, ExplainRequest request) {
        if (request.includeCustomAttributes()) {
            dataPlaneAuthzService.requireScope("explain:pii");
        }
        FeatureFlagEntity flagEntity = requireFlag(request.appId(), flagKey);
        SnapshotResponse snapshot = resolveCurrentSnapshot(request.environment(), request.appId());
        Map<String, Object> flagSnapshot = findFlagSnapshot(snapshot, flagKey).orElse(Map.of());

        return buildResponse(
                flagKey,
                flagEntity,
                request.environment(),
                snapshot.configVersion(),
                flagSnapshot,
                toContext(request.context()),
                request.includeCustomAttributes(),
                "live");
    }

    @Transactional(readOnly = true)
    public ExplainResponse replay(String flagKey, ReplayExplainRequest request) {
        if (request.includeCustomAttributes()) {
            dataPlaneAuthzService.requireScope("explain:pii");
        }
        if (request.configVersion() == null && request.timestamp() == null) {
            throw new FmsException(
                    FmsErrorCode.VALIDATION_ERROR,
                    "Either configVersion or timestamp is required for replay.");
        }

        FeatureFlagEntity flagEntity = requireFlag(request.appId(), flagKey);
        long configVersion = resolveReplayConfigVersion(request.environment(), request.configVersion(), request.timestamp());
        Map<String, Object> flagSnapshot = loadFlagSnapshotAtVersion(
                request.appId(), flagKey, request.environment(), configVersion);

        return buildResponse(
                flagKey,
                flagEntity,
                request.environment(),
                configVersion,
                flagSnapshot,
                toContext(request.context()),
                request.includeCustomAttributes(),
                "replay");
    }

    private ExplainResponse buildResponse(
            String flagKey,
            FeatureFlagEntity flagEntity,
            String environment,
            long configVersion,
            Map<String, Object> flagSnapshot,
            EvaluationContext context,
            boolean includeCustomAttributes,
            String evaluationMode) {
        ExplainResult explained = ruleEngine.explain(flagKey, context, flagSnapshot);
        List<TraceStep> trace = new ArrayList<>(explained.decisionTrace());

        String type = flagSnapshot.containsKey("type")
                ? stringValue(flagSnapshot.get("type"))
                : flagEntity.getType().externalName();

        if ("published".equalsIgnoreCase(stringValue(flagSnapshot.get("status")))) {
            Optional<KillSwitchOverrideEntity> killSwitch = resolveKillSwitch(flagEntity.getId(), environment, context.region());
            int killSwitchIndex = Math.min(1, trace.size());
            if (killSwitch.isPresent()) {
                trace.add(killSwitchIndex, new TraceStep(
                        "kill_switch_check",
                        null,
                        null,
                        "fail",
                        "active kill switch (" + killSwitch.get().getScope().name() + ")"));
                Object forcedValue = killSwitch.get().getForcedValue();
                boolean enabled = DefaultRuleEngine.computeEnabled(type, forcedValue, ReasonCode.KILL_SWITCH);
                return new ExplainResponse(
                        flagKey,
                        enabled,
                        forcedValue,
                        type,
                        configVersion,
                        resolveRelease(flagSnapshot),
                        maskContext(context, includeCustomAttributes),
                        null,
                        toResponseTrace(trace),
                        null,
                        ReasonCode.KILL_SWITCH,
                        evaluationMode,
                        SCHEMA_VERSION);
            }
            trace.add(killSwitchIndex, new TraceStep(
                    "kill_switch_check",
                    null,
                    null,
                    "pass",
                    "no active kill switch"));
        }

        return new ExplainResponse(
                flagKey,
                explained.enabled(),
                explained.value(),
                type,
                configVersion,
                resolveRelease(flagSnapshot),
                maskContext(context, includeCustomAttributes),
                explained.bucket(),
                toResponseTrace(trace),
                explained.matchedRuleId(),
                explained.reasonCode(),
                evaluationMode,
                SCHEMA_VERSION);
    }

    private FeatureFlagEntity requireFlag(String appId, String flagKey) {
        return featureFlagRepository.findByApplication_SlugAndKey(appId, flagKey)
                .orElseThrow(() -> new FmsException(FmsErrorCode.FLAG_NOT_FOUND, "Flag not found: " + flagKey));
    }

    private SnapshotResponse resolveCurrentSnapshot(String environment, String appId) {
        long currentVersion = snapshotLoaderService.resolveCurrentVersion(environment, appId);
        if (currentVersion <= 0) {
            return new SnapshotResponse(environment, appId, 0L, null, "full", null, List.of(), List.of());
        }
        return snapshotCacheService.getSnapshot(environment, appId, currentVersion)
                .orElseGet(() -> snapshotLoaderService.loadFullSnapshot(environment, appId));
    }

    private long resolveReplayConfigVersion(String environment, Long configVersion, Instant timestamp) {
        if (configVersion != null) {
            return configVersion;
        }
        return configVersionHistoryRepository
                .findTopByEnvironmentAndCreatedAtLessThanEqualOrderByConfigVersionDesc(environment, timestamp)
                .map(entry -> entry.getConfigVersion())
                .orElseThrow(() -> new FmsException(
                        FmsErrorCode.VERSION_NOT_FOUND,
                        "No config version found at or before timestamp."));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadFlagSnapshotAtVersion(
            String appId, String flagKey, String environment, long configVersion) {
        return flagVersionRepository
                .findTopByFlag_Application_SlugAndFlag_KeyAndEnvironmentAndConfigVersionOrderByFlagVersionDesc(
                        appId, flagKey, environment, configVersion)
                .map(version -> {
                    Object snapshot = version.getSnapshot();
                    if (snapshot instanceof Map<?, ?> map) {
                        return new LinkedHashMap<>((Map<String, Object>) map);
                    }
                    throw new FmsException(FmsErrorCode.VERSION_NOT_FOUND, "Flag snapshot not found at version.");
                })
                .orElseThrow(() -> new FmsException(
                        FmsErrorCode.VERSION_NOT_FOUND,
                        "Flag snapshot not found for config version: " + configVersion));
    }

    private static Optional<Map<String, Object>> findFlagSnapshot(SnapshotResponse snapshot, String flagKey) {
        if (snapshot.deletedFlagKeys().contains(flagKey)) {
            return Optional.empty();
        }
        return snapshot.flags().stream()
                .filter(flag -> flagKey.equals(flag.key()))
                .findFirst()
                .map(ExplainService::toFlagMap);
    }

    private static Map<String, Object> toFlagMap(SnapshotResponse.FlagSnapshot flag) {
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

    private Optional<KillSwitchOverrideEntity> resolveKillSwitch(UUID flagId, String environment, String region) {
        List<KillSwitchOverrideEntity> active = killSwitchOverrideRepository
                .findByFlag_IdAndEnvironmentAndActiveTrue(flagId, environment);

        Optional<KillSwitchOverrideEntity> global = active.stream()
                .filter(override -> override.getScope() == KillSwitchScope.global)
                .findFirst();
        if (global.isPresent()) {
            return global;
        }

        return active.stream()
                .filter(override -> override.getScope() == KillSwitchScope.region)
                .filter(override -> region != null && region.equals(override.getRegionCode()))
                .findFirst();
    }

    private ExplainResponse.ReleaseInfo resolveRelease(Map<String, Object> flagSnapshot) {
        String releaseId = stringValue(flagSnapshot.get("releaseId"));
        if (releaseId == null) {
            return null;
        }
        return releaseRepository.findByReleaseId(releaseId)
                .map(release -> new ExplainResponse.ReleaseInfo(release.getReleaseId(), release.getVersion()))
                .orElse(new ExplainResponse.ReleaseInfo(releaseId, null));
    }

    private Map<String, Object> maskContext(EvaluationContext context, boolean includeCustomAttributes) {
        Map<String, Object> masked = new LinkedHashMap<>();
        if (context.userId() != null) {
            masked.put("userId", maskUserId(context.userId()));
        }
        if (context.region() != null) {
            masked.put("region", context.region());
        }
        if (context.appVersion() != null) {
            masked.put("appVersion", context.appVersion());
        }
        if (includeCustomAttributes && context.customAttributes() != null && !context.customAttributes().isEmpty()) {
            masked.put("customAttributes", context.customAttributes());
        }
        return masked;
    }

    private static String maskUserId(String userId) {
        if (userId.length() <= 4) {
            return "usr_***";
        }
        return userId.substring(0, 4) + "***";
    }

    private static List<ExplainResponse.DecisionStep> toResponseTrace(List<TraceStep> trace) {
        return trace.stream()
                .map(step -> new ExplainResponse.DecisionStep(
                        step.step(),
                        step.ruleId(),
                        step.ruleName(),
                        step.result(),
                        step.detail()))
                .toList();
    }

    private EvaluationContext toContext(ExplainRequest.EvaluateContextDto dto) {
        return new EvaluationContext(
                dto.userId(),
                dto.deviceId(),
                dto.region(),
                dto.appVersion(),
                dto.customAttributes() == null ? Map.of() : dto.customAttributes());
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
