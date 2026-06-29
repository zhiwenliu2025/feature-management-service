package com.fms.evaluate;

import com.fms.sync.SyncIntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

public abstract class EvaluateIntegrationTestSupport extends SyncIntegrationTestSupport {

    protected static final String API_KEY_HEADER = "Authorization";
    protected static final String API_KEY_VALUE = "ApiKey test-eval-key";

    @BeforeEach
    void setUpEvaluateTest(@Autowired MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }
}
