package com.systemforge.backend.admin.controller;

import com.systemforge.backend.admin.dto.PlatformStatsDto;
import com.systemforge.backend.admin.service.AdminService;
import com.systemforge.backend.common.dto.ApiResponse;
import com.systemforge.backend.user.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Admin-only management endpoints.
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Platform administration endpoints — ADMIN role required")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/stats")
    @Operation(summary = "Platform statistics", description = "Returns aggregated platform usage stats")
    public ResponseEntity<ApiResponse<PlatformStatsDto>> getPlatformStats() {
        return ResponseEntity.ok(ApiResponse.success("Platform stats retrieved",
                adminService.getPlatformStats()));
    }

    @PatchMapping("/users/{userId}/activate")
    @Operation(summary = "Activate user account")
    public ResponseEntity<ApiResponse<UserResponse>> activateUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.success("User activated",
                adminService.activateUser(userId)));
    }

    @PatchMapping("/users/{userId}/deactivate")
    @Operation(summary = "Deactivate user account")
    public ResponseEntity<ApiResponse<UserResponse>> deactivateUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.success("User deactivated",
                adminService.deactivateUser(userId)));
    }

    @PatchMapping("/users/{userId}/role")
    @Operation(summary = "Change user role")
    public ResponseEntity<ApiResponse<UserResponse>> changeUserRole(
            @PathVariable UUID userId,
            @RequestParam String role) {
        return ResponseEntity.ok(ApiResponse.success("User role updated",
                adminService.changeUserRole(userId, role)));
    }
}