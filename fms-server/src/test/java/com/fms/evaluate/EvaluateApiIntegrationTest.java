package com.fms.evaluate;

import com.fms.evaluate.dto.BatchEvaluateRequest;
import com.fms.evaluate.dto.EvaluateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EvaluateApiIntegrationTest extends EvaluateIntegrationTestSupport {

    @Test
    void evaluateSingleFlagReturnsRuleMatchForPublishedFlag() throws Exception {
        String flagKey = uniqueKey("eval_flag");

        createFlag(SEED_APP, flagKey).andExpect(status().isCreated());
        replaceRules(SEED_APP, flagKey, "dev");
        long configVersion = publishAndProcess(SEED_APP, flagKey, "dev");

        EvaluateRequest request = new EvaluateRequest(
                "dev",
                SEED_APP,
                null,
                new EvaluateRequest.EvaluationContextDto("usr_123", null, "US", "3.2.1", Map.of()));

        mockMvc.perform(post("/api/v1/evaluate/flags/{flagKey}", flagKey)
                        .header(API_KEY_HEADER, API_KEY_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flagKey").value(flagKey))
                .andExpect(jsonPath("$.value").value(true))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.type").value("boolean"))
                .andExpect(jsonPath("$.configVersion").value((int) configVersion))
                .andExpect(jsonPath("$.evaluationMode").value("remote"))
                .andExpect(jsonPath("$.reasonCode").value("RULE_MATCH"))
                .andExpect(jsonPath("$.latencyMs").value(greaterThanOrEqualTo(0)));
    }

    @Test
    void evaluateSingleFlagReturnsDefaultValueWhenRegionDoesNotMatch() throws Exception {
        String flagKey = uniqueKey("default_flag");

        createFlag(SEED_APP, flagKey).andExpect(status().isCreated());
        replaceRules(SEED_APP, flagKey, "dev");
        publishAndProcess(SEED_APP, flagKey, "dev");

        EvaluateRequest request = new EvaluateRequest(
                "dev",
                SEED_APP,
                null,
                new EvaluateRequest.EvaluationContextDto("usr_123", null, "EU", "3.2.1", Map.of()));

        mockMvc.perform(post("/api/v1/evaluate/flags/{flagKey}", flagKey)
                        .header(API_KEY_HEADER, API_KEY_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(false))
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.reasonCode").value("NO_MATCH"));
    }

    @Test
    void evaluateSingleFlagReturnsNotFoundForUnknownFlag() throws Exception {
        EvaluateRequest request = new EvaluateRequest(
                "dev",
                SEED_APP,
                null,
                new EvaluateRequest.EvaluationContextDto("usr_123", null, "US", null, Map.of()));

        mockMvc.perform(post("/api/v1/evaluate/flags/{flagKey}", "does_not_exist")
                        .header(API_KEY_HEADER, API_KEY_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("FLAG_NOT_FOUND"));
    }

    @Test
    void evaluateSingleFlagReturnsNotPublishedForDraftFlag() throws Exception {
        String flagKey = uniqueKey("draft_flag");

        createFlag(SEED_APP, flagKey).andExpect(status().isCreated());

        EvaluateRequest request = new EvaluateRequest(
                "dev",
                SEED_APP,
                null,
                new EvaluateRequest.EvaluationContextDto("usr_123", null, "US", null, Map.of()));

        mockMvc.perform(post("/api/v1/evaluate/flags/{flagKey}", flagKey)
                        .header(API_KEY_HEADER, API_KEY_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(false))
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.reasonCode").value("NOT_PUBLISHED"));
    }

    @Test
    void evaluateBatchReturnsMixedResults() throws Exception {
        String publishedFlag = uniqueKey("batch_published");
        String draftFlag = uniqueKey("batch_draft");

        createFlag(SEED_APP, publishedFlag).andExpect(status().isCreated());
        replaceRules(SEED_APP, publishedFlag, "dev");
        long configVersion = publishAndProcess(SEED_APP, publishedFlag, "dev");

        createFlag(SEED_APP, draftFlag).andExpect(status().isCreated());

        BatchEvaluateRequest request = new BatchEvaluateRequest(
                "dev",
                SEED_APP,
                List.of(publishedFlag, draftFlag, "missing_flag"),
                new EvaluateRequest.EvaluationContextDto("usr_123", null, "US", "3.2.1", Map.of()));

        mockMvc.perform(post("/api/v1/evaluate/batch")
                        .header(API_KEY_HEADER, API_KEY_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configVersion").value((int) configVersion))
                .andExpect(jsonPath("$.evaluationMode").value("remote"))
                .andExpect(jsonPath("$.results", hasSize(3)))
                .andExpect(jsonPath("$.results[0].flagKey").value(publishedFlag))
                .andExpect(jsonPath("$.results[0].reasonCode").value("RULE_MATCH"))
                .andExpect(jsonPath("$.results[1].reasonCode").value("NOT_PUBLISHED"))
                .andExpect(jsonPath("$.results[2].reasonCode").value("NOT_PUBLISHED"))
                .andExpect(jsonPath("$.latencyMs").value(greaterThanOrEqualTo(0)));
    }

    @Test
    void evaluateBatchRejectsMoreThanFiftyFlags() throws Exception {
        List<String> flagKeys = java.util.stream.IntStream.range(0, 51)
                .mapToObj(i -> "flag_" + i)
                .toList();

        BatchEvaluateRequest request = new BatchEvaluateRequest(
                "dev",
                SEED_APP,
                flagKeys,
                new EvaluateRequest.EvaluationContextDto("usr_123", null, "US", null, Map.of()));

        mockMvc.perform(post("/api/v1/evaluate/batch")
                        .header(API_KEY_HEADER, API_KEY_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void evaluateRequiresEnvironmentAppIdAndContext() throws Exception {
        mockMvc.perform(post("/api/v1/evaluate/flags/{flagKey}", "any_flag")
                        .header(API_KEY_HEADER, API_KEY_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void evaluateSupportsPinnedConfigVersion() throws Exception {
        String flagKey = uniqueKey("pinned_flag");

        createFlag(SEED_APP, flagKey).andExpect(status().isCreated());
        replaceRules(SEED_APP, flagKey, "dev");
        long configVersion = publishAndProcess(SEED_APP, flagKey, "dev");

        EvaluateRequest request = new EvaluateRequest(
                "dev",
                SEED_APP,
                configVersion,
                new EvaluateRequest.EvaluationContextDto("usr_123", null, "US", "3.2.1", Map.of()));

        mockMvc.perform(post("/api/v1/evaluate/flags/{flagKey}", flagKey)
                        .header(API_KEY_HEADER, API_KEY_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configVersion").value((int) configVersion))
                .andExpect(jsonPath("$.reasonCode").value("RULE_MATCH"));
    }
}
