package com.fms.platform;

import com.fms.platform.dto.HealthResponse;
import com.fms.platform.dto.ReadinessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Locale;

@RestController
@Tag(name = "Platform", description = "Operational and discovery endpoints")
public class PlatformController {

    private final PlatformHealthService healthService;
    private final PlatformOpenApiService openApiService;

    public PlatformController(PlatformHealthService healthService, PlatformOpenApiService openApiService) {
        this.healthService = healthService;
        this.openApiService = openApiService;
    }

    @GetMapping("/health")
    @Operation(summary = "Liveness probe", description = "Returns UP when the process is running.")
    HealthResponse health() {
        return new HealthResponse("UP", Instant.now());
    }

    @GetMapping("/ready")
    @Operation(summary = "Readiness probe", description = "Checks PostgreSQL and Redis connectivity.")
    ResponseEntity<ReadinessResponse> ready() {
        ReadinessResponse response = healthService.readiness();
        return "READY".equals(response.status())
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(503).body(response);
    }

    @GetMapping(value = "/v1/openapi.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "OpenAPI specification", description = "Aggregated OpenAPI 3.1 document for all modules.")
    ResponseEntity<byte[]> openapi(HttpServletRequest request, Locale locale) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(openApiService.openApiDocument(request, locale));
    }
}
