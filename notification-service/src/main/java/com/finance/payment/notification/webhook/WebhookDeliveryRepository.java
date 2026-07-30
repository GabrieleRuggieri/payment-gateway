package com.finance.payment.notification.webhook;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Coda persistente delle consegne webhook (retry + DLQ). */
@Repository
public class WebhookDeliveryRepository {

    private final JdbcTemplate jdbc;

    public WebhookDeliveryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Inserisce una delivery se non esiste già (dedup payment_id+event_type). */
    public boolean enqueue(
            UUID merchantId,
            UUID paymentId,
            String eventType,
            String payloadJson,
            String destinationUrl) {
        int rows = jdbc.update(
                """
                INSERT INTO webhook_deliveries
                    (merchant_id, payment_id, event_type, payload, destination_url, status, attempts, next_attempt_at)
                VALUES (?, ?, ?, CAST(? AS jsonb), ?, 'PENDING', 0, NOW())
                ON CONFLICT (payment_id, event_type) DO NOTHING
                """,
                merchantId, paymentId, eventType, payloadJson, destinationUrl);
        return rows > 0;
    }

    public List<Delivery> findDue(int limit) {
        return jdbc.query(
                """
                SELECT id, merchant_id, payment_id, event_type, payload::text AS payload,
                       destination_url, attempts, max_attempts
                FROM webhook_deliveries
                WHERE status = 'PENDING' AND next_attempt_at <= NOW()
                ORDER BY next_attempt_at
                LIMIT ?
                FOR UPDATE SKIP LOCKED
                """,
                (rs, rowNum) -> new Delivery(
                        rs.getLong("id"),
                        UUID.fromString(rs.getString("merchant_id")),
                        UUID.fromString(rs.getString("payment_id")),
                        rs.getString("event_type"),
                        rs.getString("payload"),
                        rs.getString("destination_url"),
                        rs.getInt("attempts"),
                        rs.getInt("max_attempts")),
                limit);
    }

    public void markDelivered(long id, String signature) {
        jdbc.update(
                """
                UPDATE webhook_deliveries
                SET status = 'DELIVERED', delivered_at = NOW(), signature = ?, last_error = NULL
                WHERE id = ?
                """,
                signature, id);
    }

    public void markRetry(long id, int attempts, Instant nextAttempt, String error) {
        jdbc.update(
                """
                UPDATE webhook_deliveries
                SET attempts = ?, next_attempt_at = ?, last_error = ?
                WHERE id = ?
                """,
                attempts, Timestamp.from(nextAttempt), truncate(error), id);
    }

    public void markDead(long id, String error) {
        jdbc.update(
                """
                UPDATE webhook_deliveries
                SET status = 'DEAD', last_error = ?, attempts = attempts
                WHERE id = ?
                """,
                truncate(error), id);
    }

    private static String truncate(String error) {
        if (error == null) {
            return null;
        }
        return error.length() > 1000 ? error.substring(0, 1000) : error;
    }

    public record Delivery(
            long id,
            UUID merchantId,
            UUID paymentId,
            String eventType,
            String payloadJson,
            String destinationUrl,
            int attempts,
            int maxAttempts) {
    }
}
