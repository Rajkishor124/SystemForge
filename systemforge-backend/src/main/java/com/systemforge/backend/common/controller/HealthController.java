package com.systemforge.backend.common.controller;

import com.systemforge.backend.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Baseline health check controller.
 *
 * <p>Used by load balancers and monitoring systems to verify the application is alive.
 * This is NOT a replacement for Spring Boot Actuator's /actuator/health — it provides
 * a simple, authenticated-accessible ping endpoint for clients.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Health", description = "Application health and readiness checks")
@Slf4j
public class HealthController {

    @GetMapping("/health")
    @Operation(
            summary = "Health check",
            description = "Returns 200 OK if the application is running. Used by monitoring systems."
    )
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        log.debug("Health check requested");
        return ResponseEntity.ok(
                ApiResponse.success("SystemForge is running", Map.of(
                        "status", "UP",
                        "service", "systemforge-backend",
                        "version", "1.0.0"
                ))
        );
    }
}