package com.finance.payment.common.saga;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * PostgreSQL-backed saga dedup: INSERT ON CONFLICT DO NOTHING.
 * Chosen over Redis for durability and auditability in payment flows.
 *
 * <p><strong>Propagation.MANDATORY</strong> is intentional: callers MUST be executed within an
 * active transaction so that the dedup INSERT and the downstream business logic are committed
 * atomically. If the caller's transaction rolls back (e.g. an exception during processing),
 * the dedup row is also rolled back, allowing Kafka to retry the event correctly. Without this
 * constraint a separate commit of the dedup row would prevent retries from re-processing the
 * event, silently routing it to the DLT after exhausting attempts.
 */
@RequiredArgsConstructor
public class SagaEventDedupService {

    private static final String INSERT_SQL = """
            INSERT INTO saga_processed_events (consumer_group, payment_id, event_type)
            VALUES (?, ?, ?)
            ON CONFLICT (consumer_group, payment_id, event_type) DO NOTHING
            """;

    private final JdbcTemplate jdbcTemplate;

    /**
     * Registers this event as processed within the caller's active transaction.
     *
     * @return {@code true} if the event is new and should be processed; {@code false} if it is a duplicate
     * @throws org.springframework.transaction.IllegalTransactionStateException if no active transaction exists
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean registerIfNew(String consumerGroup, UUID paymentId, String eventType) {
        int inserted = jdbcTemplate.update(INSERT_SQL, consumerGroup, paymentId, eventType);
        return inserted > 0;
    }
}
