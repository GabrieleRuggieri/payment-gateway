package com.finance.payment.admin;

import com.finance.payment.api.dto.MerchantWebhookResponse;
import com.finance.payment.api.dto.UpsertMerchantWebhookRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/** CRUD minimo su {@code merchant_webhooks} per demo locale. */
@Service
@RequiredArgsConstructor
public class MerchantWebhookAdminService {

    private final JdbcTemplate jdbc;

    public MerchantWebhookResponse get(UUID merchantId) {
        return find(merchantId).orElseThrow(() ->
                new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Merchant webhook config not found: " + merchantId));
    }

    @Transactional
    public MerchantWebhookResponse upsert(UUID merchantId, UpsertMerchantWebhookRequest request) {
        boolean active = request.active() == null || request.active();
        jdbc.update("""
                INSERT INTO merchant_webhooks (merchant_id, webhook_url, webhook_secret, active)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (merchant_id) DO UPDATE SET
                    webhook_url = EXCLUDED.webhook_url,
                    webhook_secret = EXCLUDED.webhook_secret,
                    active = EXCLUDED.active
                """, merchantId, request.webhookUrl(), request.webhookSecret(), active);
        return get(merchantId);
    }

    private Optional<MerchantWebhookResponse> find(UUID merchantId) {
        return jdbc.query("""
                SELECT merchant_id, webhook_url, webhook_secret, active
                FROM merchant_webhooks
                WHERE merchant_id = ?
                """, rs -> {
            if (!rs.next()) {
                return Optional.empty();
            }
            String secret = rs.getString("webhook_secret");
            String hint = secret == null || secret.length() < 4
                    ? "****"
                    : "****" + secret.substring(secret.length() - 4);
            return Optional.of(new MerchantWebhookResponse(
                    UUID.fromString(rs.getString("merchant_id")),
                    rs.getString("webhook_url"),
                    rs.getBoolean("active"),
                    hint
            ));
        }, merchantId);
    }
}
