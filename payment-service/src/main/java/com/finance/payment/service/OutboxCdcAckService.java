package com.finance.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quando l'outbox è pubblicato da Debezium CDC (relay polling disabilitato),
 * marca come {@code PUBLISHED} le righe PENDING abbastanza vecchie da essere già state lette dal connector.
 * Accettabile in locale; in produzione si preferisce delete-after-route o sink dedicato.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "payment.outbox.cdc-ack.enabled", havingValue = "true")
public class OutboxCdcAckService {

    private final JdbcTemplate jdbc;

    @Value("${payment.outbox.cdc-ack.min-age-seconds:2}")
    private int minAgeSeconds;

    @Scheduled(fixedDelayString = "${payment.outbox.cdc-ack.fixed-delay-ms:3000}")
    @Transactional
    public void ackPublished() {
        int updated = jdbc.update("""
                UPDATE payment_outbox
                SET status = 'PUBLISHED', processed_at = NOW()
                WHERE status = 'PENDING'
                  AND created_at < NOW() - (? * INTERVAL '1 second')
                """, minAgeSeconds);
        if (updated > 0) {
            log.debug("CDC-ack marked {} outbox rows as PUBLISHED", updated);
        }
    }
}
