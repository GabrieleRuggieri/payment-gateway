package com.finance.payment.api.dto;

/**
 * Wraps an idempotent operation outcome so callers can distinguish first execution from replay.
 *
 * @param <T> response type (e.g. {@link PaymentResponse})
 * @param value   serialized business response
 * @param replayed {@code true} when the idempotency key was seen before within the TTL window
 */
public record IdempotentResult<T>(T value, boolean replayed) {

    public static <T> IdempotentResult<T> fresh(T value) {
        return new IdempotentResult<>(value, false);
    }

    public static <T> IdempotentResult<T> replay(T value) {
        return new IdempotentResult<>(value, true);
    }
}
