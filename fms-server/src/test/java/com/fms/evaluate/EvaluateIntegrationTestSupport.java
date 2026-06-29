package com.fms.evaluate;

import com.fms.management.dto.CreateApiKeyRequest;
import com.fms.sync.SyncIntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class EvaluateIntegrationTestSupport extends SyncIntegrationTestSupport {

    protected static final String API_KEY_HEADER = "Authorization";

    protected String apiKeyAuthorization;

    @BeforeEach
    void setUpEvaluateTest(@Autowired MockMvc mockMvc) throws Exception {
        this.mockMvc = mockMvc;
        this.apiKeyAuthorization = "ApiKey " + createDataPlaneApiKey();
    }

    private String createDataPlaneApiKey() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/management/applications/{appId}/api-keys", SEED_APP)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateApiKeyRequest(
                                "evaluate-integration-test",
                                List.of("sync", "evaluate", "explain:read"),
                                null))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("apiKey").asText();
    }
}
