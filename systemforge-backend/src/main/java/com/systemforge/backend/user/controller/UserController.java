package com.systemforge.backend.user.controller;

import com.systemforge.backend.common.dto.ApiResponse;
import com.systemforge.backend.common.dto.PagedResponse;
import com.systemforge.backend.user.dto.request.ChangePasswordRequest;
import com.systemforge.backend.user.dto.request.UpdateProfileRequest;
import com.systemforge.backend.user.dto.response.UserResponse;
import com.systemforge.backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile management endpoints")
public class UserController {

    private final UserService userService;

    // ================= SELF =================

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Fetch authenticated user profile")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        return ResponseEntity.ok(
                ApiResponse.success("User retrieved", userService.getCurrentUser())
        );
    }

    @PutMapping("/profile")
    @Operation(summary = "Update profile", description = "Update current user's profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success("Profile updated", userService.updateProfile(request))
        );
    }

    @PutMapping("/password")
    @Operation(summary = "Change password", description = "Change user password securely")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {

        userService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password updated successfully"));
    }

    // ================= ADMIN =================

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user by ID", description = "ADMIN: Retrieve user profile by UUID")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID userId) {
        return ResponseEntity.ok(
                ApiResponse.success("User retrieved", userService.findById(userId))
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all users", description = "ADMIN: Paginated list of users")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        PagedResponse<UserResponse> users =
                userService.findAll(PageRequest.of(page, size, sort));

        return ResponseEntity.ok(ApiResponse.success("Users retrieved", users));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete user", description = "ADMIN: Soft delete user account")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully"));
    }
}