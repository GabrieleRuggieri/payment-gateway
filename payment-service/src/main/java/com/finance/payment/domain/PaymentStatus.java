package com.finance.payment.domain;

/** Stati possibili del ciclo di vita di un pagamento nella saga. */
public enum PaymentStatus {
    INITIATED,
    AUTHORIZED,
    CAPTURED,
    SETTLED,
    FAILED,
    REFUNDED
}
