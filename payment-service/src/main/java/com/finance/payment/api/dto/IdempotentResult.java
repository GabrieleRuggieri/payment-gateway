package com.finance.payment.api.dto;

/**
 * Incapsula l'esito di un'operazione idempotente per distinguere la prima esecuzione dal replay.
 *
 * @param <T>      tipo della risposta (es. {@link PaymentResponse})
 * @param value    risposta di business serializzata
 * @param replayed {@code true} se la chiave di idempotenza era già stata vista entro la finestra TTL
 */
public record IdempotentResult<T>(T value, boolean replayed) {

    public static <T> IdempotentResult<T> fresh(T value) {
        return new IdempotentResult<>(value, false);
    }

    public static <T> IdempotentResult<T> replay(T value) {
        return new IdempotentResult<>(value, true);
    }
}
