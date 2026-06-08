package com.finance.payment.api.dto;

import com.finance.payment.domain.Payment;
import com.finance.payment.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

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
