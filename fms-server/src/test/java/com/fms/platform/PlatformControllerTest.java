package com.fms.platform;

import com.fms.testsupport.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlatformControllerTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        resetRedis();
    }

    @Test
    void healthReturnsUp() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.timestamp").value(notNullValue()));
    }

    @Test
    void readyReturnsReadyWhenDependenciesAreUp() throws Exception {
        mockMvc.perform(get("/api/ready"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.checks.postgresql").value("UP"))
                .andExpect(jsonPath("$.checks.redis").value("UP"));
    }

    @Test
    void openapiReturnsDocument() throws Exception {
        mockMvc.perform(get("/api/v1/openapi.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").value("3.1.0"))
                .andExpect(jsonPath("$.info.title").value("Feature Management Service API"))
                .andExpect(jsonPath("$.paths").value(notNullValue()));
    }
}
