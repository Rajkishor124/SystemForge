package com.systemforge.backend.auth.repository;

import com.systemforge.backend.auth.entity.OtpRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpRecordRepository extends JpaRepository<OtpRecord, UUID> {

    /**
     * Finds the latest valid OTP (unused + not expired).
     */
    Optional<OtpRecord> findTopByEmailAndIsUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            String email,
            LocalDateTime now
    );

    /**
     * Invalidates all unused OTPs for an email.
     */
    @Modifying
    @org.springframework.data.jpa.repository.Query(
            "UPDATE OtpRecord o SET o.isUsed = true WHERE o.email = :email AND o.isUsed = false"
    )
    int invalidateAllForEmail(@org.springframework.data.repository.query.Param("email") String email);
}