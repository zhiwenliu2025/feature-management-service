package com.fms.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fms.management.dto.CreateFlagRequest;
import com.fms.management.dto.PublishFlagRequest;
import com.fms.management.dto.ReplaceRulesRequest;
import com.fms.management.dto.RuleInput;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import com.redis.testcontainers.RedisContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class ManagementApiIntegrationTest {

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
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Test
    void managementFlagLifecycle() throws Exception {
        String flagKey = "checkout_v2_test";

        mockMvc.perform(post("/api/v1/management/flags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateFlagRequest(
                                "checkout-service",
                                flagKey,
                                "Checkout V2 Flow",
                                "Test flag",
                                "boolean",
                                false,
                                null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value(flagKey))
                .andExpect(jsonPath("$.status").value("draft"));

        mockMvc.perform(get("/api/v1/management/flags/{flagKey}", flagKey)
                        .param("appId", "checkout-service"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value(flagKey));

        mockMvc.perform(put("/api/v1/management/flags/{flagKey}/rules", flagKey)
                        .param("appId", "checkout-service")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReplaceRulesRequest(
                                "prod",
                                List.of(new RuleInput(
                                        10,
                                        "NA rollout",
                                        Map.of(
                                                "region", Map.of("operator", "in", "values", List.of("US")),
                                                "rolloutPercent", 5),
                                        true,
                                        true))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rules.prod[0].priority").value(10));

        mockMvc.perform(post("/api/v1/management/flags/{flagKey}/publish", flagKey)
                        .param("appId", "checkout-service")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PublishFlagRequest(
                                "prod", null, "Initial publish", false))))
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
}
