package com.fms.evaluate.service;

import com.fms.cache.SnapshotCacheService;
import com.fms.common.exception.FmsErrorCode;
import com.fms.common.exception.FmsException;
import com.fms.evaluate.dto.BatchEvaluateRequest;
import com.fms.evaluate.dto.EvaluateRequest;
import com.fms.observability.FmsMetrics;
import com.fms.repository.FeatureFlagRepository;
import com.fms.ruleengine.EvaluationContext;
import com.fms.ruleengine.EvaluationResult;
import com.fms.ruleengine.ReasonCode;
import com.fms.ruleengine.RuleEngine;
import com.fms.sync.dto.SnapshotResponse;
import com.fms.sync.service.SnapshotLoaderService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluateServiceTest {

    private static final String ENV = "dev";
    private static final String APP_ID = "checkout-service";

    @Mock
    private RuleEngine ruleEngine;

    @Mock
    private SnapshotCacheService snapshotCacheService;

    @Mock
    private SnapshotLoaderService snapshotLoaderService;

    @Mock
    private FeatureFlagRepository featureFlagRepository;

    private EvaluateService evaluateService;

    @BeforeEach
    void setUp() {
        evaluateService = new EvaluateService(
                ruleEngine,
                snapshotCacheService,
                snapshotLoaderService,
                featureFlagRepository,
                new FmsMetrics(new SimpleMeterRegistry()));
    }

    @Test
    void evaluateUsesPinnedConfigVersionFromCache() {
        SnapshotResponse snapshot = snapshotWithFlag("checkout_v2", 42L);
        EvaluateRequest request = requestWithContext(42L, "US");

        when(snapshotCacheService.getSnapshot(ENV, APP_ID, 42L)).thenReturn(Optional.of(snapshot));
        when(ruleEngine.evaluate(eq("checkout_v2"), any(EvaluationContext.class), any()))
                .thenReturn(new EvaluationResult(true, true, ReasonCode.RULE_MATCH, null));

        var response = evaluateService.evaluate("checkout_v2", request);

        assertEquals(42L, response.configVersion());
        assertEquals("remote", response.evaluationMode());
        assertEquals(ReasonCode.RULE_MATCH, response.reasonCode());
        assertEquals(true, response.value());
    }

    @Test
    void evaluateThrowsWhenPinnedVersionMissing() {
        EvaluateRequest request = requestWithContext(99L, "US");

        when(snapshotCacheService.getSnapshot(ENV, APP_ID, 99L)).thenReturn(Optional.empty());
        when(snapshotLoaderService.resolveCurrentVersion(ENV, APP_ID)).thenReturn(10L);

        FmsException ex = assertThrows(
                FmsException.class, () -> evaluateService.evaluate("checkout_v2", request));

        assertEquals(FmsErrorCode.VERSION_NOT_FOUND, ex.errorCode());
    }

    @Test
    void evaluateThrowsWhenFlagDoesNotExist() {
        SnapshotResponse snapshot = new SnapshotResponse(
                ENV, APP_ID, 5L, null, "full", Instant.now(), List.of(), List.of());
        EvaluateRequest request = requestWithContext(null, "US");

        when(snapshotLoaderService.resolveCurrentVersion(ENV, APP_ID)).thenReturn(5L);
        when(snapshotCacheService.getSnapshot(ENV, APP_ID, 5L)).thenReturn(Optional.of(snapshot));
        when(featureFlagRepository.findByApplication_SlugAndKey(APP_ID, "missing_flag"))
                .thenReturn(Optional.empty());

        FmsException ex = assertThrows(
                FmsException.class, () -> evaluateService.evaluate("missing_flag", request));

        assertEquals(FmsErrorCode.FLAG_NOT_FOUND, ex.errorCode());
    }

    @Test
    void evaluateBatchReturnsNotPublishedForUnknownFlagsWithout404() {
        SnapshotResponse snapshot = snapshotWithFlag("published_flag", 7L);
        BatchEvaluateRequest request = new BatchEvaluateRequest(
                ENV,
                APP_ID,
                List.of("published_flag", "unknown_flag"),
                new EvaluateRequest.EvaluationContextDto("usr_1", null, "US", "3.2.1", Map.of()));

        when(snapshotLoaderService.resolveCurrentVersion(ENV, APP_ID)).thenReturn(7L);
        when(snapshotCacheService.getSnapshot(ENV, APP_ID, 7L)).thenReturn(Optional.of(snapshot));
        when(ruleEngine.evaluate(eq("published_flag"), any(EvaluationContext.class), any()))
                .thenReturn(new EvaluationResult(false, false, ReasonCode.DEFAULT_VALUE, null));

        var response = evaluateService.evaluateBatch(request);

        assertEquals(7L, response.configVersion());
        assertEquals(2, response.results().size());
        assertEquals(ReasonCode.NOT_PUBLISHED, response.results().get(1).reasonCode());
        verify(ruleEngine).evaluate(eq("published_flag"), any(EvaluationContext.class), any());
    }

    @Test
    void evaluateBatchMarksDeletedFlagsAsArchived() {
        SnapshotResponse snapshot = new SnapshotResponse(
                ENV,
                APP_ID,
                3L,
                null,
                "full",
                Instant.now(),
                List.of(),
                List.of("retired_flag"));

        BatchEvaluateRequest request = new BatchEvaluateRequest(
                ENV,
                APP_ID,
                List.of("retired_flag"),
                new EvaluateRequest.EvaluationContextDto("usr_1", null, "US", null, Map.of()));

        when(snapshotLoaderService.resolveCurrentVersion(ENV, APP_ID)).thenReturn(3L);
        when(snapshotCacheService.getSnapshot(ENV, APP_ID, 3L)).thenReturn(Optional.of(snapshot));

        var response = evaluateService.evaluateBatch(request);

        assertEquals(ReasonCode.ARCHIVED, response.results().get(0).reasonCode());
        assertEquals(false, response.results().get(0).enabled());
    }

    private static EvaluateRequest requestWithContext(Long configVersion, String region) {
        return new EvaluateRequest(
                ENV,
                APP_ID,
                configVersion,
                new EvaluateRequest.EvaluationContextDto("usr_1", null, region, "3.2.1", Map.of()));
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
