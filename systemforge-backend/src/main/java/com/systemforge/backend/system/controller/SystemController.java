package com.systemforge.backend.system.controller;

import com.systemforge.backend.auth.service.SecurityService;
import com.systemforge.backend.common.dto.ApiResponse;
import com.systemforge.backend.common.dto.PagedResponse;
import com.systemforge.backend.common.enums.SystemType;
import com.systemforge.backend.system.dto.SystemDefinitionDto;
import com.systemforge.backend.system.dto.UserSystemConfigDto;
import com.systemforge.backend.system.dto.request.CreateSystemConfigRequest;
import com.systemforge.backend.system.service.SystemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/systems")
@RequiredArgsConstructor
@Tag(name = "System Engineering", description = "System selection and architecture configuration endpoints")
public class SystemController {

    private final SystemService systemService;
    private final SecurityService securityService;

    // ================= PUBLIC CATALOG =================

    @GetMapping
    @SecurityRequirements // Open endpoint
    @Operation(summary = "List architectures", description = "Fetch all available system templates")
    public ResponseEntity<ApiResponse<List<SystemDefinitionDto>>> getAllSystems(
            @RequestParam(required = false) SystemType type) {

        List<SystemDefinitionDto> systems = type == null
                ? systemService.getAllSystems()
                : systemService.getSystemsByType(type);

        return ResponseEntity.ok(ApiResponse.success("Systems retrieved", systems));
    }

    // ================= USER CONFIGURATIONS =================

    @PostMapping("/configs")
    @Operation(summary = "Initialize architecture", description = "Create a new scalable system configuration for the user")
    public ResponseEntity<ApiResponse<UserSystemConfigDto>> createConfig(
            @Valid @RequestBody CreateSystemConfigRequest request) {

        UUID userId = securityService.getAuthenticatedUserId();
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("System configuration created", 
                        systemService.createConfig(userId, request)));
    }

    @GetMapping("/configs")
    @Operation(summary = "List user configurations", description = "Retrieve paginated history of user's architectures")
    public ResponseEntity<ApiResponse<PagedResponse<UserSystemConfigDto>>> getUserConfigs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        UUID userId = securityService.getAuthenticatedUserId();
        
        return ResponseEntity.ok(ApiResponse.success("Configs retrieved",
                systemService.getUserConfigs(userId, PageRequest.of(page, size, Sort.by("createdAt").descending()))));
    }

    @GetMapping("/configs/{configId}")
    @Operation(summary = "Get user configuration by ID", description = "Fetch specific architecture and its AI-generated payload")
    public ResponseEntity<ApiResponse<UserSystemConfigDto>> getConfigById(
            @PathVariable UUID configId) {

        UUID userId = securityService.getAuthenticatedUserId();
        
        return ResponseEntity.ok(ApiResponse.success("Config retrieved",
                systemService.getConfigById(userId, configId)));
    }

    @PostMapping("/configs/{configId}/generate")
    @Operation(summary = "Trigger generation", description = "Generate architecture topology and configuration payloads via AI")
    public ResponseEntity<ApiResponse<UserSystemConfigDto>> generateArchitecture(
            @PathVariable UUID configId) {

        UUID userId = securityService.getAuthenticatedUserId();
        
        return ResponseEntity.ok(ApiResponse.success("Architecture generated successfully",
                systemService.generateArchitecture(userId, configId)));
    }

    @DeleteMapping("/configs/{configId}")
    @Operation(summary = "Delete configuration", description = "Soft delete a specific architecture configuration")
    public ResponseEntity<ApiResponse<Void>> deleteConfig(
            @PathVariable UUID configId) {

        UUID userId = securityService.getAuthenticatedUserId();
        
        systemService.deleteConfig(userId, configId);
        
        return ResponseEntity.ok(ApiResponse.success("System configuration deleted"));
    }
}
