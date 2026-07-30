package com.finance.payment.notification.webhook;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Accesso JDBC a {@code merchant_webhooks}. */
@Repository
public class MerchantWebhookRepository {

    private final JdbcTemplate jdbc;

    public MerchantWebhookRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<MerchantWebhook> findActive(UUID merchantId) {
        return jdbc.query(
                """
                SELECT merchant_id, webhook_url, webhook_secret
                FROM merchant_webhooks
                WHERE merchant_id = ? AND active = TRUE
                """,
                rs -> {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(new MerchantWebhook(
                            UUID.fromString(rs.getString("merchant_id")),
                            rs.getString("webhook_url"),
                            rs.getString("webhook_secret")));
                },
                merchantId);
    }

    public record MerchantWebhook(UUID merchantId, String webhookUrl, String webhookSecret) {
    }
}
