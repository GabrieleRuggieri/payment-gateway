package com.finance.payment.common.processor;

/**
 * Esito di un'operazione verso il processore di pagamento (mock o Stripe).
 */
public record ProcessorResult(boolean success, String reference, String failureReason) {

    public static ProcessorResult ok(String reference) {
        return new ProcessorResult(true, reference, null);
    }

    public static ProcessorResult fail(String reason) {
        return new ProcessorResult(false, null, reason);
    }
}
