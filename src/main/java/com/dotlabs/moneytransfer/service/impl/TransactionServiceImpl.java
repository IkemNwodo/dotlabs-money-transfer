package com.dotlabs.moneytransfer.service.impl;

import com.dotlabs.moneytransfer.dto.request.TransactionFilterRequest;
import com.dotlabs.moneytransfer.dto.response.TransactionResponse;
import com.dotlabs.moneytransfer.entity.Transaction;
import com.dotlabs.moneytransfer.exception.MoneyTransferException;
import com.dotlabs.moneytransfer.repository.TransactionRepository;
import com.dotlabs.moneytransfer.repository.specification.TransactionSpecification;
import com.dotlabs.moneytransfer.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactions(TransactionFilterRequest filter, Pageable pageable) {
        log.debug("Fetching transactions with filter: {}", filter);
        Specification<Transaction> spec = TransactionSpecification.withFilter(filter);
        return transactionRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionByReference(String transactionReference) {
        log.debug("Fetching transaction by reference: {}", transactionReference);
        Transaction transaction = transactionRepository.findByTransactionReference(transactionReference)
                .orElseThrow(() -> new MoneyTransferException("Transaction not found for reference: " + transactionReference));
        return toResponse(transaction);
    }

    public TransactionResponse toResponse(Transaction tx) {
        return TransactionResponse.builder()
                .id(tx.getId())
                .transactionReference(tx.getTransactionReference())
                .sourceAccountNumber(tx.getSourceAccountNumber())
                .destinationAccountNumber(tx.getDestinationAccountNumber())
                .amount(tx.getAmount())
                .transactionFee(tx.getTransactionFee())
                .billedAmount(tx.getBilledAmount())
                .description(tx.getDescription())
                .status(tx.getStatus())
                .statusMessage(tx.getStatusMessage())
                .commissionWorthy(tx.getCommissionWorthy())
                .commission(tx.getCommission())
                .commissionProcessedAt(tx.getCommissionProcessedAt())
                .dateCreated(tx.getDateCreated())
                .build();
    }
}
