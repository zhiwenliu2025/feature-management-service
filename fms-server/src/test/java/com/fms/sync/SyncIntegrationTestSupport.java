package com.fms.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fms.management.dto.CreateApiKeyRequest;
import com.fms.testsupport.IntegrationTestSupport;
import com.fms.worker.PublishWorkerService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class SyncIntegrationTestSupport extends IntegrationTestSupport {

    protected static final String API_KEY_HEADER = "Authorization";

    @DynamicPropertySource
    static void configureSyncProperties(DynamicPropertyRegistry registry) {
        registry.add("fms.worker.publish.poll-interval-ms", () -> "60000");
        registry.add("fms.sync.delta-max-gap", () -> "5");
    }

    @Autowired
    protected PublishWorkerService publishWorkerService;

    protected String dataPlaneApiKeyAuthorization;

    @BeforeEach
    void setUpSyncTest(@Autowired MockMvc mockMvc) throws Exception {
        this.mockMvc = mockMvc;
        resetRedis();
        publishWorkerService.processPendingJobs();
        this.dataPlaneApiKeyAuthorization = "ApiKey " + createDataPlaneApiKey();
    }

    protected long publishAndProcess(String appId, String flagKey, String environment) throws Exception {
        MvcResult publishResult = publishFlag(appId, flagKey, environment).andExpect(status().isAccepted()).andReturn();
        JsonNode body = objectMapper.readTree(publishResult.getResponse().getContentAsString());
        publishWorkerService.processPendingJobs();
        return body.get("configVersion").asLong();
    }

    private String createDataPlaneApiKey() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/management/applications/{appId}/api-keys", SEED_APP)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateApiKeyRequest(
                                "sync-integration-test",
                                List.of("sync", "evaluate", "explain:read"),
                                null))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("apiKey").asText();
    }

    protected MockHttpServletRequestBuilder withDataPlaneAuth(MockHttpServletRequestBuilder builder) {
        return builder.header(API_KEY_HEADER, dataPlaneApiKeyAuthorization);
    }
}
