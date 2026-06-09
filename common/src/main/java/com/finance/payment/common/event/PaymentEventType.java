package com.finance.payment.common.event;

/**
 * Tipi canonici di eventi della saga di pagamento pubblicati su {@code payment.events}.
 */
public enum PaymentEventType {

    PAYMENT_INITIATED("PaymentInitiated"),
    PAYMENT_AUTHORIZED("PaymentAuthorized"),
    AUTHORIZATION_FAILED("AuthorizationFailed"),
    PAYMENT_CAPTURED("PaymentCaptured"),
    CAPTURE_FAILED("CaptureFailed"),
    PAYMENT_SETTLED("PaymentSettled"),
    SETTLEMENT_FAILED("SettlementFailed"),
    PAYMENT_REFUNDED("PaymentRefunded");

    private final String wireName;

    PaymentEventType(String wireName) {
        this.wireName = wireName;
    }

    /** Restituisce il nome serializzato sull'event bus. */
    public String wireName() {
        return wireName;
    }

    /** Risolve il tipo enum dal nome wire; solleva eccezione se sconosciuto. */
    public static PaymentEventType fromWireName(String wireName) {
        for (PaymentEventType type : values()) {
            if (type.wireName.equals(wireName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown event type: " + wireName);
    }
}
