package com.finance.payment.domain;

/** Stato di elaborazione di un record nella tabella outbox. */
public enum OutboxStatus {
    PENDING,
    PROCESSING,
    PUBLISHED,
    FAILED
}
