package com.fms.management;

import com.fms.management.dto.CreateApiKeyRequest;
import com.fms.management.dto.CreateApplicationRequest;
import com.fms.management.dto.CreateReleaseRequest;
import com.fms.management.dto.KillSwitchRequest;
import com.fms.management.dto.LinkFlagsRequest;
import com.fms.management.dto.PromoteRequest;
import com.fms.management.dto.ReplaceRulesRequest;
import com.fms.management.dto.RollbackFlagRequest;
import com.fms.management.dto.RuleInput;
import com.fms.management.dto.UpdateApplicationRequest;
import com.fms.management.dto.UpdateFlagRequest;
import com.fms.management.dto.UpdateRuleRequest;
import com.fms.sync.SyncIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ManagementApiIntegrationTest extends SyncIntegrationTestSupport {

    @Test
    void managementFlagLifecycle() throws Exception {
        String flagKey = uniqueKey("checkout_v2");

        createFlag(SEED_APP, flagKey)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value(flagKey))
                .andExpect(jsonPath("$.status").value("draft"));

        mockMvc.perform(get("/api/v1/management/flags/{flagKey}", flagKey)
                        .param("appId", SEED_APP))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value(flagKey));

        replaceRules(SEED_APP, flagKey, "prod");

        publishFlag(SEED_APP, flagKey, "prod")
                .andExpect(status().isAccepted())
                .andExpect(header().exists("X-Config-Version"))
                .andExpect(jsonPath("$.status").value("pending"))
                .andExpect(jsonPath("$.flagVersion").value(1));

        mockMvc.perform(get("/api/v1/management/environments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").exists());

        mockMvc.perform(get("/api/v1/management/audit")
                        .param("resourceType", "feature_flag")
                        .param("resourceId", flagKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void listAndGetSeedApplication() throws Exception {
        mockMvc.perform(get("/api/v1/management/applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.slug == 'checkout-service')]").exists());

        mockMvc.perform(get("/api/v1/management/applications/{appId}", SEED_APP))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value(SEED_APP))
                .andExpect(jsonPath("$.name").value("Checkout Service"));
    }

    @Test
    void createUpdateApplicationAndManageApiKeys() throws Exception {
        String slug = uniqueSlug("payments");

        mockMvc.perform(post("/api/v1/management/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateApplicationRequest(
                                slug,
                                "Payments Service",
                                "Payments integration test app",
                                "platform"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value(slug))
                .andExpect(jsonPath("$.status").value("active"));

        mockMvc.perform(put("/api/v1/management/applications/{appId}", slug)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateApplicationRequest(
                                "Payments Service Updated",
                                "Updated description",
                                "payments-team"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Payments Service Updated"))
                .andExpect(jsonPath("$.ownerTeam").value("payments-team"));

        MvcResult createKeyResult = mockMvc.perform(post("/api/v1/management/applications/{appId}/api-keys", slug)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateApiKeyRequest(
                                "CI deploy key",
                                null,
                                null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.apiKey").isNotEmpty())
                .andExpect(jsonPath("$.keyPrefix", startsWith("fms_")))
                .andReturn();

        String keyId = objectMapper.readTree(createKeyResult.getResponse().getContentAsString())
                .get("id")
                .asText();

        mockMvc.perform(get("/api/v1/management/applications/{appId}/api-keys", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(keyId))
                .andExpect(jsonPath("$[0].name").value("CI deploy key"))
                .andExpect(jsonPath("$[0].revokedAt").doesNotExist());

        mockMvc.perform(delete("/api/v1/management/applications/{appId}/api-keys/{keyId}", slug, keyId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/management/applications/{appId}/api-keys", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].revokedAt").isNotEmpty());
    }

    @Test
    void listUpdateArchiveAndVersionHistory() throws Exception {
        String flagKey = uniqueKey("list_flag");

        createFlag(SEED_APP, flagKey).andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/management/flags")
                        .param("appId", SEED_APP)
                        .param("search", flagKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].key", hasItem(flagKey)));

        mockMvc.perform(put("/api/v1/management/flags/{flagKey}", flagKey)
                        .param("appId", SEED_APP)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateFlagRequest(
                                "Renamed Flag",
                                "Updated description",
                                List.of("experiment")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed Flag"))
                .andExpect(jsonPath("$.tags[0]").value("experiment"));

        replaceRules(SEED_APP, flagKey, "prod");
        publishFlag(SEED_APP, flagKey, "prod")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.flagVersion").value(1));

        mockMvc.perform(get("/api/v1/management/flags/{flagKey}/versions", flagKey)
                        .param("appId", SEED_APP)
                        .param("environment", "prod"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].flagVersion").value(1));

        mockMvc.perform(get("/api/v1/management/flags/{flagKey}/versions/{version}", flagKey, 1)
                        .param("appId", SEED_APP)
                        .param("environment", "prod"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flagVersion").value(1))
                .andExpect(jsonPath("$.snapshot").exists());

        mockMvc.perform(delete("/api/v1/management/flags/{flagKey}", flagKey)
                        .param("appId", SEED_APP))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/management/flags/{flagKey}", flagKey)
                        .param("appId", SEED_APP))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("archived"));
    }

    @Test
    void patchRuleRollbackAndRepublish() throws Exception {
        String flagKey = uniqueKey("rollback_flag");

        createFlag(SEED_APP, flagKey).andExpect(status().isCreated());

        MvcResult rulesResult = mockMvc.perform(put("/api/v1/management/flags/{flagKey}/rules", flagKey)
                        .param("appId", SEED_APP)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReplaceRulesRequest(
                                "prod",
                                List.of(new RuleInput(
                                        10,
                                        "Original rule",
                                        Map.of("always", true),
                                        true,
                                        true))))))
                .andExpect(status().isOk())
                .andReturn();

        String ruleId = objectMapper.readTree(rulesResult.getResponse().getContentAsString())
                .at("/rules/prod/0/id")
                .asText();

        mockMvc.perform(patch("/api/v1/management/flags/{flagKey}/rules/{ruleId}", flagKey, ruleId)
                        .param("appId", SEED_APP)
                        .param("environment", "prod")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateRuleRequest(
                                20,
                                "Updated rule",
                                null,
                                false,
                                false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rules.prod[0].priority").value(20))
                .andExpect(jsonPath("$.rules.prod[0].value").value(false));

        publishFlag(SEED_APP, flagKey, "prod")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.flagVersion").value(1));
        publishWorkerService.processPendingJobs();

        publishFlag(SEED_APP, flagKey, "prod")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.flagVersion").value(2));
        publishWorkerService.processPendingJobs();

        mockMvc.perform(post("/api/v1/management/flags/{flagKey}/rollback", flagKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RollbackFlagRequest(
                                SEED_APP,
                                "prod",
                                1,
                                "Rollback to v1"))))
                .andExpect(status().isAccepted())
                .andExpect(header().exists("X-Config-Version"))
                .andExpect(jsonPath("$.flagVersion").value(3));
    }

    @Test
    void createListGetAndLinkReleaseFlags() throws Exception {
        String flagKey = uniqueKey("release_flag");
        String releaseId = uniqueSlug("rel");

        createFlag(SEED_APP, flagKey).andExpect(status().isCreated());
        replaceRules(SEED_APP, flagKey, "prod");
        publishFlag(SEED_APP, flagKey, "prod").andExpect(status().isAccepted());

        mockMvc.perform(post("/api/v1/management/releases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateReleaseRequest(
                                releaseId,
                                "2026.06.1",
                                "June release",
                                "Integration test release",
                                Map.of("team", "platform")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.releaseId").value(releaseId))
                .andExpect(jsonPath("$.version").value("2026.06.1"));

        mockMvc.perform(get("/api/v1/management/releases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.releaseId == '" + releaseId + "')]").exists());

        mockMvc.perform(get("/api/v1/management/releases/{releaseId}", releaseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.releaseId").value(releaseId))
                .andExpect(jsonPath("$.flags").isArray());

        mockMvc.perform(post("/api/v1/management/releases/{releaseId}/flags", releaseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LinkFlagsRequest(
                                List.of(flagKey),
                                SEED_APP,
                                "prod"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flags[0].flagKey").value(flagKey))
                .andExpect(jsonPath("$.flags[0].environment").value("prod"));
    }

    @Test
    void getEnvironmentConfigAndPromote() throws Exception {
        String flagKey = uniqueKey("promote_flag");

        createFlag(SEED_APP, flagKey).andExpect(status().isCreated());
        replaceRules(SEED_APP, flagKey, "dev");
        publishFlag(SEED_APP, flagKey, "dev")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.environment").value("dev"));

        mockMvc.perform(get("/api/v1/management/environments/{env}/config", "dev"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.environment").value("dev"))
                .andExpect(jsonPath("$.currentConfigVersion").isNumber());

        mockMvc.perform(post("/api/v1/management/environments/{env}/promote", "staging")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PromoteRequest(
                                "dev",
                                List.of(flagKey),
                                SEED_APP,
                                null,
                                "Promote to staging"))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.targetEnvironment").value("staging"))
                .andExpect(jsonPath("$.sourceEnvironment").value("dev"))
                .andExpect(jsonPath("$.promotedFlags[0]").value(flagKey))
                .andExpect(jsonPath("$.publishJobIds").isArray());
    }

    @Test
    void activateListAndDeactivateKillSwitch() throws Exception {
        String flagKey = uniqueKey("kill_switch_flag");

        createFlag(SEED_APP, flagKey).andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/management/flags/{flagKey}/kill-switch", flagKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new KillSwitchRequest(
                                SEED_APP,
                                "prod",
                                "global",
                                null,
                                false,
                                "Emergency off"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flagKey").value(flagKey))
                .andExpect(jsonPath("$.scope").value("global"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.forcedValue").value(false));

        mockMvc.perform(get("/api/v1/management/flags/{flagKey}/kill-switch", flagKey)
                        .param("appId", SEED_APP)
                        .param("environment", "prod"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overrides").isArray())
                .andExpect(jsonPath("$.overrides[0].isActive").value(true));

        mockMvc.perform(delete("/api/v1/management/flags/{flagKey}/kill-switch", flagKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new KillSwitchRequest(
                                SEED_APP,
                                "prod",
                                "global",
                                null,
                                false,
                                null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));

        mockMvc.perform(get("/api/v1/management/flags/{flagKey}/kill-switch", flagKey)
                        .param("appId", SEED_APP)
                        .param("environment", "prod"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overrides").isEmpty());
    }
}
