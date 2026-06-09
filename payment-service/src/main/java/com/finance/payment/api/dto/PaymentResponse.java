package com.finance.payment.api.dto;

import com.finance.payment.domain.Payment;
import com.finance.payment.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** DTO di risposta con lo stato corrente di un pagamento. */
public record PaymentResponse(
        UUID id,
        UUID merchantId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String description,
        Map<String, Object> metadata,
        Instant createdAt,
        Instant updatedAt
) {

    /** Mappa l'entità di dominio {@link Payment} nel DTO di risposta. */
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getMerchantId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getDescription(),
                payment.getMetadata(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
