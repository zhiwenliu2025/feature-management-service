package com.fms.evaluate;

import com.fms.sync.SyncIntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;

public abstract class EvaluateIntegrationTestSupport extends SyncIntegrationTestSupport {

    protected static final String API_KEY_HEADER = SyncIntegrationTestSupport.API_KEY_HEADER;

    protected String apiKeyAuthorization;

    @BeforeEach
    void setUpEvaluateTest() {
        this.apiKeyAuthorization = dataPlaneApiKeyAuthorization;
    }
}
