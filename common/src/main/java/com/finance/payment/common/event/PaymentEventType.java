package com.finance.payment.common.event;

/**
 * Canonical payment saga event types published on {@code payment.events}.
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

    public String wireName() {
        return wireName;
    }

    public static PaymentEventType fromWireName(String wireName) {
        for (PaymentEventType type : values()) {
            if (type.wireName.equals(wireName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown event type: " + wireName);
    }
}
