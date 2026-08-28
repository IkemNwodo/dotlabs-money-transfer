package com.dotlabs.moneytransfer.exception;

public class AccountNotFoundException extends MoneyTransferException {
    public AccountNotFoundException(String message) {
        super(message);
    }
}
