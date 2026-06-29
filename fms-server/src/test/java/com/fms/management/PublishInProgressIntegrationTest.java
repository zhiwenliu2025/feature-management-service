package com.fms.management;

import com.fms.sync.SyncIntegrationTestSupport;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PublishInProgressIntegrationTest extends SyncIntegrationTestSupport {

    @Test
    void rejectsSecondPublishWhileJobIsPending() throws Exception {
        String flagKey = uniqueKey("pending_publish_flag");

        createFlag(SEED_APP, flagKey).andExpect(status().isCreated());
        replaceRules(SEED_APP, flagKey, "dev");

        publishFlag(SEED_APP, flagKey, "dev")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("pending"));

        publishFlag(SEED_APP, flagKey, "dev")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PUBLISH_IN_PROGRESS"));
    }

    @Test
    void allowsPublishAfterPendingJobCompletes() throws Exception {
        String flagKey = uniqueKey("completed_publish_flag");

        createFlag(SEED_APP, flagKey).andExpect(status().isCreated());
        replaceRules(SEED_APP, flagKey, "dev");

        publishFlag(SEED_APP, flagKey, "dev").andExpect(status().isAccepted());
        publishWorkerService.processPendingJobs();

        publishFlag(SEED_APP, flagKey, "dev").andExpect(status().isAccepted());
    }
}
