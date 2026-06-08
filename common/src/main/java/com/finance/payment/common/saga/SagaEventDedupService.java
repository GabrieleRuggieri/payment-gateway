package com.finance.payment.common.saga;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * PostgreSQL-backed saga dedup: INSERT ON CONFLICT DO NOTHING.
 * Chosen over Redis for durability and auditability in payment flows.
 */
@Service
@RequiredArgsConstructor
public class SagaEventDedupService {

    private static final String INSERT_SQL = """
            INSERT INTO saga_processed_events (consumer_group, payment_id, event_type)
            VALUES (?, ?, ?)
            ON CONFLICT (consumer_group, payment_id, event_type) DO NOTHING
            """;

    private final JdbcTemplate jdbcTemplate;

    /**
     * @return true if this consumer has not processed this event yet
     */
    @Transactional
    public boolean registerIfNew(String consumerGroup, UUID paymentId, String eventType) {
        int inserted = jdbcTemplate.update(INSERT_SQL, consumerGroup, paymentId, eventType);
        return inserted > 0;
    }
}
