package com.finance.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Scheduler per la retention delle righe di deduplicazione saga su PostgreSQL. */
@Component
@RequiredArgsConstructor
@Slf4j
public class SagaDedupCleanupScheduler {

    private final JdbcTemplate jdbcTemplate;

    @Value("${payment.cleanup.saga-retention-days:30}")
    private int sagaRetentionDays;

    /** Elimina le righe di dedup saga più vecchie del periodo di retention configurato. */
    @Scheduled(cron = "${payment.cleanup.saga-cron:0 30 3 * * *}")
    public void purgeOldDedupRows() {
        Instant cutoff = Instant.now().minus(sagaRetentionDays, ChronoUnit.DAYS);
        int deleted = jdbcTemplate.update(
                "DELETE FROM saga_processed_events WHERE processed_at < ?",
                cutoff
        );
        if (deleted > 0) {
            log.info("Purged {} saga dedup rows older than {} days", deleted, sagaRetentionDays);
        }
    }
}
