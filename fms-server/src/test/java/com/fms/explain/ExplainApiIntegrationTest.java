package com.fms.explain;

import com.fms.explain.dto.ExplainRequest;
import com.fms.explain.dto.ReplayExplainRequest;
import com.fms.evaluate.EvaluateIntegrationTestSupport;
import com.fms.management.dto.KillSwitchRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExplainApiIntegrationTest extends EvaluateIntegrationTestSupport {

    @Test
    void explainReturnsDecisionTraceForPublishedFlag() throws Exception {
        String flagKey = uniqueKey("explain_flag");

        createFlag(SEED_APP, flagKey).andExpect(status().isCreated());
        replaceRules(SEED_APP, flagKey, "dev");
        long configVersion = publishAndProcess(SEED_APP, flagKey, "dev");

        ExplainRequest request = new ExplainRequest(
                "dev",
                SEED_APP,
                new ExplainRequest.EvaluateContextDto("usr_12345", null, "US", "3.2.1", Map.of()),
                false);

        mockMvc.perform(post("/api/v1/explain/flags/{flagKey}", flagKey)
                        .header(API_KEY_HEADER, API_KEY_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flagKey").value(flagKey))
                .andExpect(jsonPath("$.value").value(true))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.type").value("boolean"))
                .andExpect(jsonPath("$.configVersion").value((int) configVersion))
                .andExpect(jsonPath("$.evaluationMode").value("live"))
                .andExpect(jsonPath("$.reasonCode").value("RULE_MATCH"))
                .andExpect(jsonPath("$.schemaVersion").value("1.0"))
                .andExpect(jsonPath("$.context.userId").value("usr_***"))
                .andExpect(jsonPath("$.context.region").value("US"))
                .andExpect(jsonPath("$.decisionTrace[*].step", hasItem("environment_check")))
                .andExpect(jsonPath("$.decisionTrace[*].step", hasItem("kill_switch_check")))
                .andExpect(jsonPath("$.decisionTrace[*].step", hasItem("rule_evaluation")));
    }

    @Test
    void explainReturnsNotFoundForUnknownFlag() throws Exception {
        ExplainRequest request = new ExplainRequest(
                "dev",
                SEED_APP,
                new ExplainRequest.EvaluateContextDto("usr_1", null, "US", null, Map.of()),
                false);

        mockMvc.perform(post("/api/v1/explain/flags/{flagKey}", "missing_flag")
                        .header(API_KEY_HEADER, API_KEY_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("FLAG_NOT_FOUND"));
    }

    @Test
    void explainReturnsNotPublishedForDraftFlag() throws Exception {
        String flagKey = uniqueKey("draft_explain_flag");

        createFlag(SEED_APP, flagKey).andExpect(status().isCreated());

        ExplainRequest request = new ExplainRequest(
                "dev",
                SEED_APP,
                new ExplainRequest.EvaluateContextDto("usr_1", null, "US", null, Map.of()),
                false);

        mockMvc.perform(post("/api/v1/explain/flags/{flagKey}", flagKey)
                        .header(API_KEY_HEADER, API_KEY_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reasonCode").value("NOT_PUBLISHED"))
                .andExpect(jsonPath("$.decisionTrace[0].step").value("environment_check"))
                .andExpect(jsonPath("$.decisionTrace[0].result").value("fail"));
    }

    @Test
    void explainReturnsNoMatchWhenRegionDoesNotMatch() throws Exception {
        String flagKey = uniqueKey("no_match_flag");

        createFlag(SEED_APP, flagKey).andExpect(status().isCreated());
        replaceRules(SEED_APP, flagKey, "dev");
        publishAndProcess(SEED_APP, flagKey, "dev");

        ExplainRequest request = new ExplainRequest(
                "dev",
                SEED_APP,
                new ExplainRequest.EvaluateContextDto("usr_1", null, "EU", null, Map.of()),
                false);

        mockMvc.perform(post("/api/v1/explain/flags/{flagKey}", flagKey)
                        .header(API_KEY_HEADER, API_KEY_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(false))
                .andExpect(jsonPath("$.reasonCode").value("NO_MATCH"));
    }

    @Test
    void explainReflectsActiveKillSwitch() throws Exception {
        String flagKey = uniqueKey("kill_switch_explain");

        createFlag(SEED_APP, flagKey).andExpect(status().isCreated());
        replaceRules(SEED_APP, flagKey, "dev");
        publishAndProcess(SEED_APP, flagKey, "dev");

        mockMvc.perform(post("/api/v1/management/flags/{flagKey}/kill-switch", flagKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new KillSwitchRequest(
                                SEED_APP, "dev", "global", null, false, "test kill switch"))))
                .andExpect(status().isCreated());

        ExplainRequest request = new ExplainRequest(
                "dev",
                SEED_APP,
                new ExplainRequest.EvaluateContextDto("usr_1", null, "US", null, Map.of()),
                false);

        mockMvc.perform(post("/api/v1/explain/flags/{flagKey}", flagKey)
                        .header(API_KEY_HEADER, API_KEY_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reasonCode").value("KILL_SWITCH"))
                .andExpect(jsonPath("$.value").value(false))
                .andExpect(jsonPath("$.decisionTrace[?(@.step == 'kill_switch_check')].result").value("fail"));
    }

    @Test
    void replayUsesPinnedConfigVersion() throws Exception {
        String flagKey = uniqueKey("replay_flag");

        createFlag(SEED_APP, flagKey).andExpect(status().isCreated());
        replaceRules(SEED_APP, flagKey, "dev");
        long configVersion = publishAndProcess(SEED_APP, flagKey, "dev");

        ReplayExplainRequest request = new ReplayExplainRequest(
                "dev",
                SEED_APP,
                configVersion,
                null,
                new ExplainRequest.EvaluateContextDto("usr_1", null, "US", "3.2.1", Map.of()),
                false);

        mockMvc.perform(post("/api/v1/explain/flags/{flagKey}/replay", flagKey)
                        .header(API_KEY_HEADER, API_KEY_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evaluationMode").value("replay"))
                .andExpect(jsonPath("$.configVersion").value((int) configVersion))
                .andExpect(jsonPath("$.reasonCode").value("RULE_MATCH"));
    }

    @Test
    void replayRejectsMissingVersionSelector() throws Exception {
        String flagKey = uniqueKey("replay_invalid");

        createFlag(SEED_APP, flagKey).andExpect(status().isCreated());

        ReplayExplainRequest request = new ReplayExplainRequest(
                "dev",
                SEED_APP,
                null,
                null,
                new ExplainRequest.EvaluateContextDto("usr_1", null, "US", null, Map.of()),
                false);

        mockMvc.perform(post("/api/v1/explain/flags/{flagKey}/replay", flagKey)
                        .header(API_KEY_HEADER, API_KEY_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }
}
