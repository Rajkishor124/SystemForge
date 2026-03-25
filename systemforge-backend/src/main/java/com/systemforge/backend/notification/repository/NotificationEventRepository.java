package com.systemforge.backend.notification.repository;

import com.systemforge.backend.notification.entity.NotificationEvent;
import com.systemforge.backend.notification.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationEventRepository extends JpaRepository<NotificationEvent, UUID> {

    Page<NotificationEvent> findByUserId(UUID userId, Pageable pageable);

    /** Used by the retry scheduler — fetch all failed events under max retry threshold. */
    List<NotificationEvent> findByStatusAndRetryCountLessThan(
            NotificationStatus status, int maxRetries);
}