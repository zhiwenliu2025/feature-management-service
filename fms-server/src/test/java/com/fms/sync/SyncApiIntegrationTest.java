package com.fms.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fms.management.ManagementIntegrationTestSupport;
import com.fms.worker.PublishWorkerService;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class SyncApiIntegrationTest extends ManagementIntegrationTestSupport {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:18"));

    @Container
    static RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:8"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("fms.worker.publish.poll-interval-ms", () -> "60000");
        registry.add("fms.sync.delta-max-gap", () -> "5");
    }

    @Autowired
    private PublishWorkerService publishWorkerService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void injectMockMvc(@Autowired MockMvc mockMvc) {
        this.mockMvc = mockMvc;
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
    }

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
                .andExpect(jsonPath("$.flags[*].key", hasItem(flagKey)));
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
                .andExpect(jsonPath("$.flags[*].key", hasItem(flagKey)));
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

    private long publishAndProcess(String appId, String flagKey, String environment) throws Exception {
        MvcResult publishResult = publishFlag(appId, flagKey, environment).andExpect(status().isAccepted()).andReturn();
        JsonNode body = objectMapper.readTree(publishResult.getResponse().getContentAsString());
        publishWorkerService.processPendingJobs();
        return body.get("configVersion").asLong();
    }
}
