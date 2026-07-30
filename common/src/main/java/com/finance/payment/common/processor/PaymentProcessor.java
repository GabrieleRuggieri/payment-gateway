package com.finance.payment.common.processor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Contratto verso il processore esterno (authorize → capture → settle / compensazioni).
 */
public interface PaymentProcessor {

    /** Blocca i fondi (Stripe: PaymentIntent con {@code capture_method=manual}). */
    ProcessorResult authorize(UUID paymentId, BigDecimal amount, String currency, String paymentMethodId);

    /** Annulla un'autorizzazione non ancora catturata (Stripe: cancel PaymentIntent). */
    ProcessorResult voidAuthorization(UUID paymentId, String authorizationCode);

    /** Addebita i fondi autorizzati (Stripe: capture PaymentIntent). */
    ProcessorResult capture(UUID paymentId, BigDecimal amount, String currency, String authorizationCode);

    /**
     * Conferma il regolamento verso il merchant.
     * Con Stripe i fondi sono già sul balance dopo la capture: verifica lo stato e restituisce il reference.
     */
    ProcessorResult settle(UUID paymentId, UUID merchantId, BigDecimal amount, String currency, String captureReference);

    /** Rimborsa dopo capture (compensazione settlement fallito). */
    ProcessorResult refund(UUID paymentId, BigDecimal amount, String currency, String captureOrPaymentIntentId);
}
