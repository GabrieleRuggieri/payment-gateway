package com.finance.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Currency;
import java.util.Map;
import java.util.UUID;

/**
 * Radice dell'aggregato pagamento — applica le transizioni di stato valide della FSM della saga.
 */
@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    private String description;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    public static Payment initiate(
            String idempotencyKey,
            UUID merchantId,
            BigDecimal amount,
            String currency,
            String description,
            Map<String, Object> metadata) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (currency == null || !isValidCurrency(currency)) {
            throw new IllegalArgumentException("Invalid currency: " + currency);
        }

        Payment payment = new Payment();
        payment.idempotencyKey = idempotencyKey;
        payment.merchantId = merchantId;
        payment.amount = amount.setScale(4, RoundingMode.UNNECESSARY);
        payment.currency = currency.toUpperCase();
        payment.description = description;
        payment.metadata = metadata;
        payment.status = PaymentStatus.INITIATED;
        return payment;
    }

    public void authorize() {
        assertStatus(PaymentStatus.INITIATED);
        this.status = PaymentStatus.AUTHORIZED;
    }

    public void capture() {
        assertStatus(PaymentStatus.AUTHORIZED);
        this.status = PaymentStatus.CAPTURED;
    }

    public void settle() {
        assertStatus(PaymentStatus.CAPTURED);
        this.status = PaymentStatus.SETTLED;
    }

    public void refund() {
        if (this.status != PaymentStatus.CAPTURED && this.status != PaymentStatus.SETTLED) {
            throw new IllegalStateException(
                    "Invalid transition: " + this.status + " → REFUNDED (expected CAPTURED or SETTLED)");
        }
        this.status = PaymentStatus.REFUNDED;
    }

    /** Segna il pagamento come contestato (dispute/chargeback) da CAPTURED o SETTLED. */
    public void dispute() {
        if (this.status != PaymentStatus.CAPTURED && this.status != PaymentStatus.SETTLED) {
            throw new IllegalStateException(
                    "Invalid transition: " + this.status + " → DISPUTED (expected CAPTURED or SETTLED)");
        }
        this.status = PaymentStatus.DISPUTED;
    }

    /** Unisce chiavi nel metadata JSON (es. processorPaymentIntentId dopo authorize). */
    public void putMetadata(String key, Object value) {
        if (key == null || value == null) {
            return;
        }
        Map<String, Object> next = this.metadata == null
                ? new java.util.HashMap<>()
                : new java.util.HashMap<>(this.metadata);
        next.put(key, value);
        this.metadata = next;
    }

    public void fail(PaymentStatus fromStatus) {
        assertStatus(fromStatus);
        this.status = PaymentStatus.FAILED;
    }

    private void assertStatus(PaymentStatus expected) {
        if (this.status != expected) {
            throw new IllegalStateException(
                    "Invalid transition: " + this.status + " → cannot proceed from " + expected);
        }
    }

    private static boolean isValidCurrency(String currency) {
        return Currency.getAvailableCurrencies().stream()
                .map(Currency::getCurrencyCode)
                .anyMatch(code -> code.equalsIgnoreCase(currency));
    }
}
