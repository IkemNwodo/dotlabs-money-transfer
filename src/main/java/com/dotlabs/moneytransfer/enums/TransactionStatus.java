package com.dotlabs.moneytransfer.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TransactionStatus {
    SUCCESSFUL("SUCCESSFUL"),
    INSUFFICIENT_FUNDS("INSUFFICIENT FUND"),
    FAILED("FAILED"),
    ACCOUNT_NOT_FOUND("ACCOUNT_NOT_FOUND"),
    INVALID_REQUEST("INVALID_REQUEST");

    private final String value;

    TransactionStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static TransactionStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (TransactionStatus status : values()) {
            if (status.name().equalsIgnoreCase(value) || status.value.equalsIgnoreCase(value)
                    || status.name().replace("_", " ").equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown transaction status: " + value);
    }
}
