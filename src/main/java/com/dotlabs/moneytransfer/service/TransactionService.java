package com.dotlabs.moneytransfer.service;

import com.dotlabs.moneytransfer.dto.request.TransactionFilterRequest;
import com.dotlabs.moneytransfer.dto.response.TransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransactionService {
    Page<TransactionResponse> getTransactions(TransactionFilterRequest filter, Pageable pageable);
    TransactionResponse getTransactionByReference(String transactionReference);
}
