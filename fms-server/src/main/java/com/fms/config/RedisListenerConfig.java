package com.fms.config;

import com.fms.sync.SseStreamManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.Topic;

import java.util.List;

@Configuration
public class RedisListenerConfig {

    private static final List<Topic> SYNC_CHANNELS = List.of(
            new ChannelTopic("fms:pubsub:dev"),
            new ChannelTopic("fms:pubsub:staging"),
            new ChannelTopic("fms:pubsub:prod"));

    @Bean
    RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            SseStreamManager sseStreamManager) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(
                (message, pattern) -> {
                    String channel = new String(message.getChannel());
                    String environment = channel.substring(channel.lastIndexOf(':') + 1);
                    sseStreamManager.handlePubSubMessage(environment, new String(message.getBody()));
                },
                SYNC_CHANNELS);
        return container;
    }
}
