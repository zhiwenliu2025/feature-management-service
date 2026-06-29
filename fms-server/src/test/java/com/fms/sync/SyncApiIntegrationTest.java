package com.fms.sync;

import com.fms.management.dto.CreateApplicationRequest;
import com.fms.sync.dto.SnapshotResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.util.zip.GZIPInputStream;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SyncApiIntegrationTest extends SyncIntegrationTestSupport {

    @Test
    void snapshotReturnsFullPayloadAfterPublish() throws Exception {
        String flagKey = uniqueKey("sync_flag");

        createFlag(SEED_APP, flagKey).andExpect(status().isCreated());
        replaceRules(SEED_APP, flagKey, "dev");
        long configVersion = publishAndProcess(SEED_APP, flagKey, "dev");

        mockMvc.perform(get("/api/v1/sync/snapshot")
                        .param("environment", "dev")
                        .param("appId", SEED_APP))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Config-Version", String.valueOf(configVersion)))
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andExpect(jsonPath("$.syncType").value("full"))
                .andExpect(jsonPath("$.environment").value("dev"))
                .andExpect(jsonPath("$.appId").value(SEED_APP))
                .andExpect(jsonPath("$.configVersion").value((int) configVersion))
                .andExpect(jsonPath("$.generatedAt").value(notNullValue()))
                .andExpect(jsonPath("$.flags[*].key", hasItem(flagKey)))
                .andExpect(jsonPath("$.deletedFlagKeys", empty()));
    }

    @Test
    void snapshotIncludesPublishedFlagStructure() throws Exception {
        String flagKey = uniqueKey("structure_flag");

        createFlag(SEED_APP, flagKey).andExpect(status().isCreated());
        replaceRules(SEED_APP, flagKey, "dev");
        publishAndProcess(SEED_APP, flagKey, "dev");

        mockMvc.perform(get("/api/v1/sync/snapshot")
                        .param("environment", "dev")
                        .param("appId", SEED_APP))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flags[?(@.key == '" + flagKey + "')].type").value("boolean"))
                .andExpect(jsonPath("$.flags[?(@.key == '" + flagKey + "')].defaultValue").value(false))
                .andExpect(jsonPath("$.flags[?(@.key == '" + flagKey + "')].status").value("published"))
                .andExpect(jsonPath("$.flags[?(@.key == '" + flagKey + "')].rolloutSalt").value(notNullValue()))
                .andExpect(jsonPath("$.flags[?(@.key == '" + flagKey + "')].rules", hasSize(1)))
                .andExpect(jsonPath("$.flags[?(@.key == '" + flagKey + "')].rules[0].priority").value(10));
    }

    @Test
    void snapshotReturnsEmptyFlagsForAppWithNoPublishedFlags() throws Exception {
        String appSlug = uniqueSlug("sync_app");

        mockMvc.perform(post("/api/v1/management/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateApplicationRequest(
                                appSlug,
                                "Sync Test App " + appSlug,
                                "No published flags",
                                null))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/sync/snapshot")
                        .param("environment", "dev")
                        .param("appId", appSlug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.syncType").value("full"))
                .andExpect(jsonPath("$.appId").value(appSlug))
                .andExpect(jsonPath("$.flags", empty()));
    }

    @Test
    void snapshotReturnsFullWhenSinceVersionIsZero() throws Exception {
        String flagKey = uniqueKey("since_zero_flag");

        createFlag(SEED_APP, flagKey).andExpect(status().isCreated());
        replaceRules(SEED_APP, flagKey, "dev");
        long configVersion = publishAndProcess(SEED_APP, flagKey, "dev");

        mockMvc.perform(get("/api/v1/sync/snapshot")
                        .param("environment", "dev")
                        .param("appId", SEED_APP)
                        .param("sinceVersion", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.syncType").value("full"))
                .andExpect(jsonPath("$.configVersion").value((int) configVersion))
                .andExpect(jsonPath("$.flags[*].key", hasItem(flagKey)));
    }

    @Test
    void snapshotSupportsGzipEncoding() throws Exception {
        String flagKey = uniqueKey("gzip_flag");

        createFlag(SEED_APP, flagKey).andExpect(status().isCreated());
        replaceRules(SEED_APP, flagKey, "dev");
        publishAndProcess(SEED_APP, flagKey, "dev");

        MvcResult result = mockMvc.perform(get("/api/v1/sync/snapshot")
                        .param("environment", "dev")
                        .param("appId", SEED_APP)
                        .header(HttpHeaders.ACCEPT_ENCODING, "gzip"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_ENCODING, "gzip"))
                .andReturn();

        SnapshotResponse snapshot;
        try (GZIPInputStream gzip = new GZIPInputStream(
                new ByteArrayInputStream(result.getResponse().getContentAsByteArray()))) {
            snapshot = objectMapper.readValue(gzip.readAllBytes(), SnapshotResponse.class);
        }

        org.junit.jupiter.api.Assertions.assertEquals("full", snapshot.syncType());
        org.junit.jupiter.api.Assertions.assertEquals(SEED_APP, snapshot.appId());
        org.junit.jupiter.api.Assertions.assertTrue(
                snapshot.flags().stream().anyMatch(flag -> flagKey.equals(flag.key())));
    }

    @Test
    void headVersionReturnsCurrentConfigVersion() throws Exception {
        String flagKey = uniqueKey("version_flag");

        createFlag(SEED_APP, flagKey).andExpect(status().isCreated());
        replaceRules(SEED_APP, flagKey, "dev");
        long configVersion = publishAndProcess(SEED_APP, flagKey, "dev");

        mockMvc.perform(head("/api/v1/sync/version")
                        .param("environment", "dev")
                        .param("appId", SEED_APP))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Config-Version", String.valueOf(configVersion)))
                .andExpect(header().string(HttpHeaders.ETAG, "\"dev:" + SEED_APP + ":" + configVersion + "\""));
    }

    @Test
    void headVersionWithoutAppIdReturnsEnvironmentVersion() throws Exception {
        String flagKey = uniqueKey("env_version_flag");

        createFlag(SEED_APP, flagKey).andExpect(status().isCreated());
        replaceRules(SEED_APP, flagKey, "dev");
        long configVersion = publishAndProcess(SEED_APP, flagKey, "dev");

        mockMvc.perform(head("/api/v1/sync/version")
                        .param("environment", "dev"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Config-Version", String.valueOf(configVersion)))
                .andExpect(header().string(HttpHeaders.ETAG, "\"dev:" + configVersion + "\""));
    }

    @Test
    void snapshotReturnsDeltaWhenSinceVersionIsRecent() throws Exception {
        String flagKey = uniqueKey("delta_flag");

        createFlag(SEED_APP, flagKey).andExpect(status().isCreated());
        replaceRules(SEED_APP, flagKey, "dev");
        long firstVersion = publishAndProcess(SEED_APP, flagKey, "dev");
        long secondVersion = publishAndProcess(SEED_APP, flagKey, "dev");

        mockMvc.perform(get("/api/v1/sync/snapshot")
                        .param("environment", "dev")
                        .param("appId", SEED_APP)
                        .param("sinceVersion", String.valueOf(firstVersion)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.syncType").value("delta"))
                .andExpect(jsonPath("$.previousVersion").value((int) firstVersion))
                .andExpect(jsonPath("$.configVersion").value((int) secondVersion))
                .andExpect(jsonPath("$.flags[*].key", hasItem(flagKey)))
                .andExpect(jsonPath("$.deletedFlagKeys", empty()));
    }

    @Test
    void snapshotReturnsNotModifiedWhenEtagMatches() throws Exception {
        String flagKey = uniqueKey("etag_flag");

        createFlag(SEED_APP, flagKey).andExpect(status().isCreated());
        replaceRules(SEED_APP, flagKey, "dev");
        long configVersion = publishAndProcess(SEED_APP, flagKey, "dev");
        String etag = "\"dev:" + SEED_APP + ":" + configVersion + "\"";

        mockMvc.perform(get("/api/v1/sync/snapshot")
                        .param("environment", "dev")
                        .param("appId", SEED_APP)
                        .param("sinceVersion", String.valueOf(configVersion))
                        .header(HttpHeaders.IF_NONE_MATCH, etag))
                .andExpect(status().isNotModified())
                .andExpect(header().string("X-Config-Version", String.valueOf(configVersion)))
                .andExpect(header().string(HttpHeaders.ETAG, etag));
    }

    @Test
    void snapshotReturnsNotModifiedWhenSinceVersionEqualsCurrent() throws Exception {
        String flagKey = uniqueKey("current_version_flag");

        createFlag(SEED_APP, flagKey).andExpect(status().isCreated());
        replaceRules(SEED_APP, flagKey, "dev");
        long configVersion = publishAndProcess(SEED_APP, flagKey, "dev");

        mockMvc.perform(get("/api/v1/sync/snapshot")
                        .param("environment", "dev")
                        .param("appId", SEED_APP)
                        .param("sinceVersion", String.valueOf(configVersion)))
                .andExpect(status().isNotModified())
                .andExpect(header().string("X-Config-Version", String.valueOf(configVersion)));
    }

    @Test
    void snapshotRejectsDeltaWhenVersionGapTooLarge() throws Exception {
        String flagKey = uniqueKey("gap_flag");

        createFlag(SEED_APP, flagKey).andExpect(status().isCreated());
        replaceRules(SEED_APP, flagKey, "dev");
        long firstVersion = publishAndProcess(SEED_APP, flagKey, "dev");

        for (int i = 0; i < 6; i++) {
            publishAndProcess(SEED_APP, flagKey, "dev");
        }

        mockMvc.perform(get("/api/v1/sync/snapshot")
                        .param("environment", "dev")
                        .param("appId", SEED_APP)
                        .param("sinceVersion", String.valueOf(firstVersion)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("DELTA_VERSION_GAP_TOO_LARGE"));
    }

    @Test
    void streamEndpointStartsAsyncSseConnection() throws Exception {
        mockMvc.perform(get("/api/v1/sync/stream")
                        .param("environment", "dev")
                        .param("appId", SEED_APP)
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, org.hamcrest.Matchers.containsString("text/event-stream")));
    }

    @Test
    void snapshotRequiresEnvironmentAndAppId() throws Exception {
        mockMvc.perform(get("/api/v1/sync/snapshot")
                        .param("appId", SEED_APP))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/sync/snapshot")
                        .param("environment", "dev"))
                .andExpect(status().isBadRequest());
    }
}
