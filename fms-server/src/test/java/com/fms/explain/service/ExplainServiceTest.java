package com.fms.explain.service;

import com.fms.cache.SnapshotCacheService;
import com.fms.common.exception.FmsErrorCode;
import com.fms.common.exception.FmsException;
import com.fms.domain.FeatureFlagEntity;
import com.fms.domain.KillSwitchOverrideEntity;
import com.fms.domain.enums.FlagType;
import com.fms.domain.enums.KillSwitchScope;
import com.fms.explain.dto.ExplainRequest;
import com.fms.explain.dto.ReplayExplainRequest;
import com.fms.repository.ConfigVersionHistoryRepository;
import com.fms.repository.FeatureFlagRepository;
import com.fms.repository.FlagVersionRepository;
import com.fms.repository.KillSwitchOverrideRepository;
import com.fms.repository.ReleaseRepository;
import com.fms.ruleengine.EvaluationContext;
import com.fms.ruleengine.ExplainResult;
import com.fms.ruleengine.ReasonCode;
import com.fms.ruleengine.RuleEngine;
import com.fms.ruleengine.TraceStep;
import com.fms.sync.dto.SnapshotResponse;
import com.fms.sync.service.SnapshotLoaderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExplainServiceTest {

    private static final String ENV = "dev";
    private static final String APP_ID = "checkout-service";
    private static final String FLAG_KEY = "checkout_v2";

    @Mock
    private RuleEngine ruleEngine;

    @Mock
    private SnapshotCacheService snapshotCacheService;

    @Mock
    private SnapshotLoaderService snapshotLoaderService;

    @Mock
    private FeatureFlagRepository featureFlagRepository;

    @Mock
    private KillSwitchOverrideRepository killSwitchOverrideRepository;

    @Mock
    private ReleaseRepository releaseRepository;

    @Mock
    private ConfigVersionHistoryRepository configVersionHistoryRepository;

    @Mock
    private FlagVersionRepository flagVersionRepository;

    private ExplainService explainService;

    @BeforeEach
    void setUp() {
        explainService = new ExplainService(
                ruleEngine,
                snapshotCacheService,
                snapshotLoaderService,
                featureFlagRepository,
                killSwitchOverrideRepository,
                releaseRepository,
                configVersionHistoryRepository,
                flagVersionRepository);
    }

    @Test
    void explainReturnsRuleMatchWithMaskedUserId() {
        FeatureFlagEntity flagEntity = flagEntity();
        SnapshotResponse snapshot = snapshotWithFlag(FLAG_KEY, 42L);

        when(featureFlagRepository.findByApplication_SlugAndKey(APP_ID, FLAG_KEY)).thenReturn(Optional.of(flagEntity));
        when(snapshotLoaderService.resolveCurrentVersion(ENV, APP_ID)).thenReturn(42L);
        when(snapshotCacheService.getSnapshot(ENV, APP_ID, 42L)).thenReturn(Optional.of(snapshot));
        when(killSwitchOverrideRepository.findByFlag_IdAndEnvironmentAndActiveTrue(flagEntity.getId(), ENV))
                .thenReturn(List.of());
        when(ruleEngine.explain(eq(FLAG_KEY), any(EvaluationContext.class), any()))
                .thenReturn(new ExplainResult(
                        true,
                        true,
                        ReasonCode.RULE_MATCH,
                        null,
                        "rule-1",
                        List.of(new TraceStep(
                                "environment_check",
                                null,
                                null,
                                "pass",
                                "published in environment"),
                                new TraceStep(
                                        "rule_evaluation",
                                        "rule-1",
                                        null,
                                        "match",
                                        "region US in [US]"))));

        ExplainRequest request = new ExplainRequest(
                ENV,
                APP_ID,
                new ExplainRequest.EvaluateContextDto("usr_12345", null, "US", "3.2.1", Map.of()),
                false);

        var response = explainService.explain(FLAG_KEY, request);

        assertEquals(FLAG_KEY, response.flagKey());
        assertEquals(true, response.value());
        assertEquals(ReasonCode.RULE_MATCH, response.reasonCode());
        assertEquals("live", response.evaluationMode());
        assertEquals("usr_***", response.context().get("userId"));
        assertEquals(3, response.decisionTrace().size());
        assertEquals("kill_switch_check", response.decisionTrace().get(1).step());
    }

    @Test
    void explainThrowsWhenFlagDoesNotExist() {
        when(featureFlagRepository.findByApplication_SlugAndKey(APP_ID, FLAG_KEY)).thenReturn(Optional.empty());

        ExplainRequest request = new ExplainRequest(
                ENV,
                APP_ID,
                new ExplainRequest.EvaluateContextDto("usr_1", null, "US", null, Map.of()),
                false);

        FmsException ex = assertThrows(FmsException.class, () -> explainService.explain(FLAG_KEY, request));
        assertEquals(FmsErrorCode.FLAG_NOT_FOUND, ex.errorCode());
    }

    @Test
    void explainAppliesKillSwitchOverride() {
        FeatureFlagEntity flagEntity = flagEntity();
        SnapshotResponse snapshot = snapshotWithFlag(FLAG_KEY, 10L);
        KillSwitchOverrideEntity override = new KillSwitchOverrideEntity();
        override.setScope(KillSwitchScope.global);
        override.setForcedValue(false);

        when(featureFlagRepository.findByApplication_SlugAndKey(APP_ID, FLAG_KEY)).thenReturn(Optional.of(flagEntity));
        when(snapshotLoaderService.resolveCurrentVersion(ENV, APP_ID)).thenReturn(10L);
        when(snapshotCacheService.getSnapshot(ENV, APP_ID, 10L)).thenReturn(Optional.of(snapshot));
        when(killSwitchOverrideRepository.findByFlag_IdAndEnvironmentAndActiveTrue(flagEntity.getId(), ENV))
                .thenReturn(List.of(override));
        when(ruleEngine.explain(eq(FLAG_KEY), any(EvaluationContext.class), any()))
                .thenReturn(new ExplainResult(
                        true,
                        true,
                        ReasonCode.RULE_MATCH,
                        null,
                        "rule-1",
                        List.of(new TraceStep("environment_check", null, null, "pass", "published in environment"))));

        ExplainRequest request = new ExplainRequest(
                ENV,
                APP_ID,
                new ExplainRequest.EvaluateContextDto("usr_1", null, "US", null, Map.of()),
                false);

        var response = explainService.explain(FLAG_KEY, request);

        assertEquals(ReasonCode.KILL_SWITCH, response.reasonCode());
        assertEquals(false, response.value());
        assertTrue(response.decisionTrace().stream()
                .anyMatch(step -> "kill_switch_check".equals(step.step()) && "fail".equals(step.result())));
    }

    @Test
    void replayRequiresConfigVersionOrTimestamp() {
        ReplayExplainRequest request = new ReplayExplainRequest(
                ENV,
                APP_ID,
                null,
                null,
                new ExplainRequest.EvaluateContextDto("usr_1", null, "US", null, Map.of()),
                false);

        FmsException ex = assertThrows(FmsException.class, () -> explainService.replay(FLAG_KEY, request));
        assertEquals(FmsErrorCode.VALIDATION_ERROR, ex.errorCode());
    }

    private static FeatureFlagEntity flagEntity() {
        FeatureFlagEntity entity = mock(FeatureFlagEntity.class);
        lenient().when(entity.getId()).thenReturn(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        lenient().when(entity.getType()).thenReturn(FlagType.boolean_);
        return entity;
    }

    private static SnapshotResponse snapshotWithFlag(String flagKey, long version) {
        SnapshotResponse.FlagSnapshot flag = new SnapshotResponse.FlagSnapshot(
                flagKey,
                "boolean",
                false,
                "published",
                "salt",
                List.of(),
                null);
        return new SnapshotResponse(
                ENV, APP_ID, version, null, "full", Instant.now(), List.of(flag), List.of());
    }
}
