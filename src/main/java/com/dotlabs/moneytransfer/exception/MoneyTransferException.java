package com.dotlabs.moneytransfer.exception;

public class MoneyTransferException extends RuntimeException {
    public MoneyTransferException(String message) {
        super(message);
    }

    public MoneyTransferException(String message, Throwable cause) {
        super(message, cause);
    }
}
