package com.finance.payment.common.exception;

/** Eccezione sollevata quando una chiave di idempotenza è già in uso con payload diverso. */
public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String message) {
        super(message);
    }
}
