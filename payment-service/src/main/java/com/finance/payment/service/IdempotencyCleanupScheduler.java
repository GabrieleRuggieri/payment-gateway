package com.finance.payment.service;

import com.finance.payment.repository.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** Scheduler per la pulizia periodica delle chiavi di idempotenza scadute. */
@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyCleanupScheduler {

    private final IdempotencyKeyRepository idempotencyKeyRepository;

    /** Elimina le chiavi di idempotenza oltre la scadenza TTL. */
    @Scheduled(cron = "${payment.cleanup.idempotency-cron:0 15 * * * *}")
    @Transactional
    public void purgeExpiredKeys() {
        int deleted = idempotencyKeyRepository.deleteByExpiresAtBefore(Instant.now());
        if (deleted > 0) {
            log.info("Purged {} expired idempotency keys", deleted);
        }
    }
}
