package com.systemforge.backend.auth.repository;

import com.systemforge.backend.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Find active (non-revoked) refresh token by hash.
     */
    Optional<RefreshToken> findByTokenHashAndIsRevokedFalse(String tokenHash);

    /**
     * Revoke all tokens for a user.
     *
     * @return number of rows updated
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.isRevoked = true WHERE rt.userId = :userId")
    int revokeAllByUserId(@Param("userId") UUID userId);

    /**
     * Bulk delete expired tokens (used in scheduler).
     *
     * @return number of rows deleted
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :cutoff")
    int deleteExpiredTokens(@Param("cutoff") LocalDateTime cutoff);
}