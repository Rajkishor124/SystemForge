package com.systemforge.backend.auth.scheduler;

import com.systemforge.backend.auth.repository.OtpRecordRepository;
import com.systemforge.backend.auth.repository.RefreshTokenRepository;
import com.systemforge.backend.notification.enums.NotificationStatus;
import com.systemforge.backend.notification.repository.NotificationEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupScheduler {

    private static final int CLEANUP_GRACE_DAYS = 1;

    private final RefreshTokenRepository refreshTokenRepository;
    private final OtpRecordRepository otpRecordRepository;
    private final NotificationEventRepository notificationEventRepository;

    @Scheduled(cron = "0 0 2 * * *", zone = "UTC")
    @Transactional
    public void purgeExpiredRefreshTokens() {

        LocalDateTime cutoff = LocalDateTime.now(ZoneOffset.UTC)
                .minusDays(CLEANUP_GRACE_DAYS);

        log.info("Purging expired refresh tokens older than {}", cutoff);

        int deleted = refreshTokenRepository.deleteExpiredTokens(cutoff);

        log.info("Purged {} expired refresh tokens", deleted);
    }

    @Scheduled(fixedDelay = 30 * 60 * 1000)
    @Transactional
    public void retryFailedNotifications() {

        var failed = notificationEventRepository
                .findByStatusAndRetryCountLessThan(NotificationStatus.FAILED, 3);

        if (!failed.isEmpty()) {
            log.info("Found {} failed notifications eligible for retry", failed.size());
            // TODO: integrate NotificationService retry with backoff
        }
    }
}