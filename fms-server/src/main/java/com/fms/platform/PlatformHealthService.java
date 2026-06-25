package com.fms.platform;

import com.fms.platform.dto.ReadinessResponse;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class PlatformHealthService {

    private final JdbcTemplate jdbcTemplate;
    private final RedisConnectionFactory redisConnectionFactory;

    public PlatformHealthService(DataSource dataSource, RedisConnectionFactory redisConnectionFactory) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.redisConnectionFactory = redisConnectionFactory;
    }

    ReadinessResponse readiness() {
        Map<String, String> checks = new LinkedHashMap<>();
        checks.put("postgresql", checkPostgres());
        checks.put("redis", checkRedis());

        boolean allUp = checks.values().stream().allMatch("UP"::equals);
        return new ReadinessResponse(allUp ? "READY" : "NOT_READY", checks);
    }

    private String checkPostgres() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return "UP";
        } catch (Exception ex) {
            return "DOWN";
        }
    }

    private String checkRedis() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            String pong = connection.ping();
            return pong != null ? "UP" : "DOWN";
        } catch (Exception ex) {
            return "DOWN";
        }
    }
}
