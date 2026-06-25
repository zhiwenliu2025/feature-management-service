package com.fms.platform;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@Hidden
public class PlatformController {

    private final JdbcTemplate jdbcTemplate;
    private final RedisConnectionFactory redisConnectionFactory;
    private final String openapiVersion;

    public PlatformController(
            DataSource dataSource,
            RedisConnectionFactory redisConnectionFactory,
            @Value("${fms.api.version:v1}") String openapiVersion) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.redisConnectionFactory = redisConnectionFactory;
        this.openapiVersion = openapiVersion;
    }

    @GetMapping("/health")
    Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "timestamp", Instant.now().toString());
    }

    @GetMapping("/ready")
    ResponseEntity<Map<String, Object>> ready() {
        Map<String, String> checks = new LinkedHashMap<>();
        checks.put("postgresql", checkPostgres());
        checks.put("redis", checkRedis());

        boolean allUp = checks.values().stream().allMatch("UP"::equals);
        Map<String, Object> body = Map.of(
                "status", allUp ? "READY" : "NOT_READY",
                "checks", checks);

        return allUp ? ResponseEntity.ok(body) : ResponseEntity.status(503).body(body);
    }

    @GetMapping("/v1/openapi.json")
    Map<String, String> openapiInfo() {
        return Map.of(
                "message", "Use /v3/api-docs for generated OpenAPI document",
                "version", openapiVersion);
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
        try {
            redisConnectionFactory.getConnection().ping();
            return "UP";
        } catch (Exception ex) {
            return "DOWN";
        }
    }
}
