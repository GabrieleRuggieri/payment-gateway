package com.finance.payment.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "merchant_api_keys")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class MerchantApiKey {

    @Id
    @Column(name = "api_key_hash", length = 64)
    private String apiKeyHash;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
