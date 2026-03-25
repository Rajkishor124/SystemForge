package com.systemforge.backend.user.repository;

import com.systemforge.backend.user.entity.User;
import com.systemforge.backend.user.enums.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data access layer for {@link User}.
 *
 * Notes:
 * - Soft delete enforced via @SQLRestriction on entity
 * - Email comparisons are case-insensitive
 * - Designed for MySQL → PostgreSQL migration
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by email (case-insensitive).
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Find active user by email.
     * Used in authentication.
     */
    Optional<User> findByEmailIgnoreCaseAndAccountStatus(String email, AccountStatus status);

    /**
     * Check if email already exists.
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Fetch users by account status (admin use).
     */
    List<User> findAllByAccountStatus(AccountStatus status);

    /**
     * Count users by account status (admin stats).
     */
    long countByAccountStatus(AccountStatus status);
}