package com.systemforge.backend.user.service;

import com.systemforge.backend.user.dto.request.ChangePasswordRequest;
import com.systemforge.backend.user.dto.request.UpdateProfileRequest;
import com.systemforge.backend.user.dto.response.UserResponse;
import org.springframework.data.domain.Pageable;

import com.systemforge.backend.common.dto.PagedResponse;
import java.util.UUID;

/**
 * Service contract for the User domain module.
 *
 * <p>All inter-module access to user data must go through this interface —
 * never by directly importing the UserRepository or User entity from outside this module.
 * This enforces clean module boundaries and enables future microservice extraction.
 *
 * <p>Implementation: {@link com.systemforge.backend.user.service.impl.UserServiceImpl}
 */
public interface UserService {

    UserResponse findById(UUID userId);

    UserResponse findByEmail(String email);

    PagedResponse<UserResponse> findAll(Pageable pageable);

    void deleteUser(UUID userId);

    // 🔥 NEW METHODS

    UserResponse getCurrentUser();

    UserResponse updateProfile(UpdateProfileRequest request);

    void changePassword(ChangePasswordRequest request);
}