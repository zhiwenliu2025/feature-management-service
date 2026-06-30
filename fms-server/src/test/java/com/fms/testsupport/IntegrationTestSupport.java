package com.fms.testsupport;

import com.fms.management.ManagementIntegrationTestSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base class for integration tests that require Postgres, Redis, and MockMvc.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
public abstract class IntegrationTestSupport extends ManagementIntegrationTestSupport {

    @DynamicPropertySource
    static void registerContainers(DynamicPropertyRegistry registry) {
        IntegrationTestContainers.register(registry);
    }

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired(required = false)
    private RedisMessageListenerContainer redisMessageListenerContainer;

    protected void resetRedis() {
        boolean wasRunning = redisMessageListenerContainer != null && redisMessageListenerContainer.isRunning();
        if (wasRunning) {
            redisMessageListenerContainer.stop();
        }
        try {
            redisTemplate.execute((RedisCallback<Object>) connection -> {
                connection.serverCommands().flushDb();
                return null;
            });
        } finally {
            if (wasRunning) {
                redisMessageListenerContainer.start();
            }
        }
    }
}
