package com.finance.payment.common.exception;

/** Eccezione sollevata quando il processore esterno non è raggiungibile o ha esaurito i tentativi. */
public class ProcessorUnavailableException extends RuntimeException {

    public ProcessorUnavailableException(String message) {
        super(message);
    }

    public ProcessorUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
