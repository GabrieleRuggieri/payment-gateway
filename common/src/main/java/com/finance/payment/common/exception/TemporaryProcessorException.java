package com.finance.payment.common.exception;

/**
 * Transient failure from external processor — safe to retry with Resilience4j.
 */
public class TemporaryProcessorException extends RuntimeException {

    public TemporaryProcessorException(String message) {
        super(message);
    }
}
