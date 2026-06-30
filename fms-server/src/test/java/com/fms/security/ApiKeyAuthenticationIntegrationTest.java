package com.fms.security;

import com.fms.management.dto.CreateApiKeyRequest;
import com.fms.sync.SyncIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = "fms.security.api-key.enforced=true")
class ApiKeyAuthenticationIntegrationTest extends SyncIntegrationTestSupport {

    @Test
    void rejectsDataPlaneRequestWithoutApiKey() throws Exception {
        mockMvc.perform(get("/api/v1/sync/snapshot")
                        .param("environment", "dev")
                        .param("appId", SEED_APP))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void rejectsInvalidApiKey() throws Exception {
        mockMvc.perform(get("/api/v1/sync/snapshot")
                        .header(HttpHeaders.AUTHORIZATION, "ApiKey invalid.key")
                        .param("environment", "dev")
                        .param("appId", SEED_APP))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void rejectsApiKeyWithoutRequiredScope() throws Exception {
        String apiKey = createApiKey(List.of("sync"));

        mockMvc.perform(post("/api/v1/evaluate/flags/any_flag")
                        .header(HttpHeaders.AUTHORIZATION, "ApiKey " + apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "environment": "dev",
                                  "appId": "checkout-service",
                                  "context": { "userId": "usr_1", "region": "US" }
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void allowsSyncWithSyncScope() throws Exception {
        String apiKey = createApiKey(List.of("sync"));

        mockMvc.perform(get("/api/v1/sync/snapshot")
                        .header(HttpHeaders.AUTHORIZATION, "ApiKey " + apiKey)
                        .param("environment", "dev")
                        .param("appId", SEED_APP))
                .andExpect(status().isOk());
    }

    @Test
    void explainRequiresExplainReadScope() throws Exception {
        String flagKey = uniqueKey("explain_scope_flag");
        String syncKey = createApiKey(List.of("sync", "evaluate"));
        String explainKey = createApiKey(List.of("sync", "evaluate", "explain:read"));

        createFlag(SEED_APP, flagKey).andExpect(status().isCreated());
        replaceRules(SEED_APP, flagKey, "dev");
        publishAndProcess(SEED_APP, flagKey, "dev");

        String body = """
                {
                  "environment": "dev",
                  "appId": "checkout-service",
                  "context": { "userId": "usr_1", "region": "US" },
                  "includeCustomAttributes": false
                }
                """;

        mockMvc.perform(post("/api/v1/explain/flags/{flagKey}", flagKey)
                        .header(HttpHeaders.AUTHORIZATION, "ApiKey " + syncKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/explain/flags/{flagKey}", flagKey)
                        .header(HttpHeaders.AUTHORIZATION, "ApiKey " + explainKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void explainCustomAttributesRequireExplainPiiScope() throws Exception {
        String flagKey = uniqueKey("explain_pii_flag");
        String explainKey = createApiKey(List.of("explain:read"));
        String explainPiiKey = createApiKey(List.of("explain:read", "explain:pii"));

        createFlag(SEED_APP, flagKey).andExpect(status().isCreated());
        replaceRules(SEED_APP, flagKey, "dev");
        publishAndProcess(SEED_APP, flagKey, "dev");

        String body = """
                {
                  "environment": "dev",
                  "appId": "checkout-service",
                  "context": {
                    "userId": "usr_1",
                    "region": "US",
                    "customAttributes": { "tier": "gold" }
                  },
                  "includeCustomAttributes": true
                }
                """;

        mockMvc.perform(post("/api/v1/explain/flags/{flagKey}", flagKey)
                        .header(HttpHeaders.AUTHORIZATION, "ApiKey " + explainKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        mockMvc.perform(post("/api/v1/explain/flags/{flagKey}", flagKey)
                        .header(HttpHeaders.AUTHORIZATION, "ApiKey " + explainPiiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.context.customAttributes.tier").value("gold"));
    }

    private String createApiKey(List<String> scopes) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/management/applications/{appId}/api-keys", SEED_APP)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateApiKeyRequest(
                                "integration-test-key",
                                scopes,
                                null))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("apiKey").asText();
    }
}
