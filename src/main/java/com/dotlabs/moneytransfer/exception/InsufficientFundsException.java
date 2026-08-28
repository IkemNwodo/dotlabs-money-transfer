package com.dotlabs.moneytransfer.exception;

public class InsufficientFundsException extends MoneyTransferException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}
