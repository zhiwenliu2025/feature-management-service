package com.fms.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fms.management.dto.CreateFlagRequest;
import com.fms.management.dto.PublishFlagRequest;
import com.fms.management.dto.ReplaceRulesRequest;
import com.fms.management.dto.RuleInput;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class ManagementIntegrationTestSupport {

    protected static final String SEED_APP = "checkout-service";

    protected final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    protected MockMvc mockMvc;

    protected String uniqueSlug(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    protected String uniqueKey(String prefix) {
        return uniqueSlug(prefix);
    }

    protected ResultActions createFlag(String appId, String flagKey) throws Exception {
        return mockMvc.perform(post("/api/v1/management/flags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateFlagRequest(
                        appId,
                        flagKey,
                        "Test Flag " + flagKey,
                        "Integration test flag",
                        "boolean",
                        false,
                        null))));
    }

    protected void replaceRules(String appId, String flagKey, String environment) throws Exception {
        mockMvc.perform(put("/api/v1/management/flags/{flagKey}/rules", flagKey)
                        .param("appId", appId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReplaceRulesRequest(
                                environment,
                                List.of(new RuleInput(
                                        10,
                                        "Test rule",
                                        Map.of("region", Map.of("operator", "in", "values", List.of("US"))),
                                        true,
                                        true))))))
                .andExpect(status().isOk());
    }

    protected ResultActions publishFlag(String appId, String flagKey, String environment) throws Exception {
        return publishFlag(appId, flagKey, environment, null);
    }

    protected ResultActions publishFlag(String appId, String flagKey, String environment, String idempotencyKey)
            throws Exception {
        var requestBuilder = post("/api/v1/management/flags/{flagKey}/publish", flagKey)
                .param("appId", appId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PublishFlagRequest(
                        environment, null, "Test publish", false)));
        if (idempotencyKey != null) {
            requestBuilder = requestBuilder.header("Idempotency-Key", idempotencyKey);
        }
        return mockMvc.perform(requestBuilder);
    }
}
