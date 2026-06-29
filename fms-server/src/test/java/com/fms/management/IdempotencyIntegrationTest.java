package com.fms.management;

import com.fms.repository.PublishJobRepository;
import com.fms.sync.SyncIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IdempotencyIntegrationTest extends SyncIntegrationTestSupport {

    @Autowired
    private PublishJobRepository publishJobRepository;

    @Test
    void publishWithSameIdempotencyKeyReturnsCachedResponseWithoutNewJob() throws Exception {
        String flagKey = uniqueKey("idempotent_publish_flag");
        String idempotencyKey = UUID.randomUUID().toString();

        createFlag(SEED_APP, flagKey).andExpect(status().isCreated());
        replaceRules(SEED_APP, flagKey, "dev");

        long jobsBefore = publishJobRepository.count();

        publishFlag(SEED_APP, flagKey, "dev", idempotencyKey)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.flagVersion").value(1))
                .andExpect(header().exists("X-Config-Version"));

        publishFlag(SEED_APP, flagKey, "dev", idempotencyKey)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.flagVersion").value(1));

        assertEquals(jobsBefore + 1, publishJobRepository.count());
    }

    @Test
    void publishWithoutIdempotencyKeyCreatesSeparateJobs() throws Exception {
        String flagKey = uniqueKey("non_idempotent_publish_flag");

        createFlag(SEED_APP, flagKey).andExpect(status().isCreated());
        replaceRules(SEED_APP, flagKey, "dev");

        long jobsBefore = publishJobRepository.count();

        publishFlag(SEED_APP, flagKey, "dev").andExpect(status().isAccepted());
        publishWorkerService.processPendingJobs();
        publishFlag(SEED_APP, flagKey, "dev").andExpect(status().isAccepted());

        assertEquals(jobsBefore + 2, publishJobRepository.count());
    }
}
