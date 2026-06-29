package com.fms.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fms.management.ManagementIntegrationTestSupport;
import com.fms.worker.PublishWorkerService;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
public abstract class SyncIntegrationTestSupport extends ManagementIntegrationTestSupport {

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
    protected PublishWorkerService publishWorkerService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUpSyncTest(@Autowired MockMvc mockMvc) {
        this.mockMvc = mockMvc;
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
    }

    protected long publishAndProcess(String appId, String flagKey, String environment) throws Exception {
        MvcResult publishResult = publishFlag(appId, flagKey, environment).andExpect(status().isAccepted()).andReturn();
        JsonNode body = objectMapper.readTree(publishResult.getResponse().getContentAsString());
        publishWorkerService.processPendingJobs();
        return body.get("configVersion").asLong();
    }
}
