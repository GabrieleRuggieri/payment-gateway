package com.finance.payment.common.exception;

public class ProcessorUnavailableException extends RuntimeException {

    public ProcessorUnavailableException(String message) {
        super(message);
    }

    public ProcessorUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
