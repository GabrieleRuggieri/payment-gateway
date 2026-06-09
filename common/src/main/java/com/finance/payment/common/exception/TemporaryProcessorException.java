package com.finance.payment.common.exception;

/** Errore transitorio del processore esterno — sicuro da ritentare con Resilience4j. */
public class TemporaryProcessorException extends RuntimeException {

    public TemporaryProcessorException(String message) {
        super(message);
    }
}
