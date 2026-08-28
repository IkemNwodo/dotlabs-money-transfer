package com.dotlabs.moneytransfer.service;

import com.dotlabs.moneytransfer.dto.request.TransferRequest;
import com.dotlabs.moneytransfer.dto.response.TransferResponse;
import com.dotlabs.moneytransfer.entity.Account;
import com.dotlabs.moneytransfer.entity.Transaction;
import com.dotlabs.moneytransfer.enums.TransactionStatus;
import com.dotlabs.moneytransfer.exception.AccountNotFoundException;
import com.dotlabs.moneytransfer.exception.InvalidTransferException;
import com.dotlabs.moneytransfer.repository.AccountRepository;
import com.dotlabs.moneytransfer.repository.TransactionRepository;
import com.dotlabs.moneytransfer.util.FeeCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Override
    @Transactional
    public TransferResponse processTransfer(TransferRequest request) {
        log.info("Processing transfer from account {} to account {} for amount {}",
                request.getSourceAccountNumber(), request.getDestinationAccountNumber(), request.getAmount());

        // Validate basic inputs
        if (request.getSourceAccountNumber().trim().equalsIgnoreCase(request.getDestinationAccountNumber().trim())) {
            throw new InvalidTransferException("Source and destination accounts cannot be the same");
        }

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransferException("Transfer amount must be strictly greater than zero");
        }

        // Generate or validate transaction reference
        String reference = (request.getTransactionReference() != null && !request.getTransactionReference().trim().isEmpty())
                ? request.getTransactionReference().trim()
                : generateReference();

        if (transactionRepository.existsByTransactionReference(reference)) {
            throw new InvalidTransferException("Transaction reference already exists: " + reference);
        }

        // Calculate transaction fee (0.5% capped at 100) and billed amount (amount + fee)
        BigDecimal fee = FeeCalculator.calculateTransactionFee(request.getAmount());
        BigDecimal billedAmount = FeeCalculator.calculateBilledAmount(request.getAmount(), fee);

        // Prevent deadlocks by acquiring pessimistic locks in a consistent alphabetical order
        Account sourceAccount;
        Account destinationAccount;

        String sourceAccNum = request.getSourceAccountNumber().trim();
        String destAccNum = request.getDestinationAccountNumber().trim();

        if (sourceAccNum.compareTo(destAccNum) < 0) {
            sourceAccount = accountRepository.findByAccountNumberWithLock(sourceAccNum)
                    .orElseThrow(() -> new AccountNotFoundException("Source account not found: " + sourceAccNum));
            destinationAccount = accountRepository.findByAccountNumberWithLock(destAccNum)
                    .orElseThrow(() -> new AccountNotFoundException("Destination account not found: " + destAccNum));
        } else {
            destinationAccount = accountRepository.findByAccountNumberWithLock(destAccNum)
                    .orElseThrow(() -> new AccountNotFoundException("Destination account not found: " + destAccNum));
            sourceAccount = accountRepository.findByAccountNumberWithLock(sourceAccNum)
                    .orElseThrow(() -> new AccountNotFoundException("Source account not found: " + sourceAccNum));
        }

        // Check for sufficient funds (including transaction fee)
        if (sourceAccount.getBalance().compareTo(billedAmount) < 0) {
            log.warn("Transfer failed due to insufficient funds: Account {} has balance {}, required {}",
                    sourceAccNum, sourceAccount.getBalance(), billedAmount);

            Transaction insufficientFundTx = Transaction.builder()
                    .transactionReference(reference)
                    .sourceAccountNumber(sourceAccNum)
                    .destinationAccountNumber(destAccNum)
                    .amount(request.getAmount())
                    .transactionFee(fee)
                    .billedAmount(billedAmount)
                    .description(request.getDescription())
                    .status(TransactionStatus.INSUFFICIENT_FUNDS)
                    .statusMessage("Insufficient funds: available balance is " + sourceAccount.getBalance()
                            + ", required billed amount is " + billedAmount)
                    .dateCreated(LocalDateTime.now())
                    .build();

            Transaction savedTx = transactionRepository.save(insufficientFundTx);
            return toTransferResponse(savedTx);
        }

        // Execute atomic debit and credit
        sourceAccount.debit(billedAmount);
        destinationAccount.credit(request.getAmount());

        accountRepository.save(sourceAccount);
        accountRepository.save(destinationAccount);

        Transaction successfulTx = Transaction.builder()
                .transactionReference(reference)
                .sourceAccountNumber(sourceAccNum)
                .destinationAccountNumber(destAccNum)
                .amount(request.getAmount())
                .transactionFee(fee)
                .billedAmount(billedAmount)
                .description(request.getDescription())
                .status(TransactionStatus.SUCCESSFUL)
                .statusMessage("Transfer completed successfully")
                .dateCreated(LocalDateTime.now())
                .build();

        Transaction savedTx = transactionRepository.save(successfulTx);

        log.info("Transfer {} completed successfully. Debited {} from {}, credited {} to {}",
                reference, billedAmount, sourceAccNum, request.getAmount(), destAccNum);

        return toTransferResponse(savedTx);
    }

    private String generateReference() {
        return "TX-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private TransferResponse toTransferResponse(Transaction tx) {
        return TransferResponse.builder()
                .transactionReference(tx.getTransactionReference())
                .sourceAccountNumber(tx.getSourceAccountNumber())
                .destinationAccountNumber(tx.getDestinationAccountNumber())
                .amount(tx.getAmount())
                .transactionFee(tx.getTransactionFee())
                .billedAmount(tx.getBilledAmount())
                .status(tx.getStatus())
                .statusMessage(tx.getStatusMessage())
                .description(tx.getDescription())
                .dateCreated(tx.getDateCreated())
                .build();
    }
}
