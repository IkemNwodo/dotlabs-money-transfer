package com.dotlabs.moneytransfer.service.impl;

import com.dotlabs.moneytransfer.dto.request.AuthorizeTransferRequest;
import com.dotlabs.moneytransfer.dto.request.InitiateTransferRequest;
import com.dotlabs.moneytransfer.dto.request.TransferRequest;
import com.dotlabs.moneytransfer.dto.response.InitiateTransferResponse;
import com.dotlabs.moneytransfer.dto.response.TransferResponse;
import com.dotlabs.moneytransfer.entity.Account;
import com.dotlabs.moneytransfer.entity.Transaction;
import com.dotlabs.moneytransfer.entity.TransferOtpSession;
import com.dotlabs.moneytransfer.entity.User;
import com.dotlabs.moneytransfer.enums.TransactionStatus;
import com.dotlabs.moneytransfer.exception.AccountNotFoundException;
import com.dotlabs.moneytransfer.exception.InvalidTransferException;
import com.dotlabs.moneytransfer.repository.AccountRepository;
import com.dotlabs.moneytransfer.repository.TransactionRepository;
import com.dotlabs.moneytransfer.security.AccountOwnershipValidator;
import com.dotlabs.moneytransfer.service.OtpService;
import com.dotlabs.moneytransfer.service.TransferService;
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
    private final AccountOwnershipValidator accountOwnershipValidator;
    private final OtpService otpService;

    @Override
    @Transactional
    public TransferResponse processTransfer(TransferRequest request) {
        // Enforce account ownership if authenticated user is present
        accountOwnershipValidator.validateOwnership(request.getSourceAccountNumber());
        return executeTransfer(request);
    }

    @Override
    @Transactional
    public InitiateTransferResponse initiateTransfer(InitiateTransferRequest request, User authenticatedUser) {
        log.info("Initiating 2FA transfer from {} to {} for amount {}",
                request.getSourceAccountNumber(), request.getDestinationAccountNumber(), request.getAmount());

        if (authenticatedUser == null) {
            throw new InvalidTransferException("Authentication required to initiate transfer");
        }

        // Verify that the user owns the source account
        accountOwnershipValidator.validateOwnership(request.getSourceAccountNumber());

        if (request.getSourceAccountNumber().trim().equalsIgnoreCase(request.getDestinationAccountNumber().trim())) {
            throw new InvalidTransferException("Source and destination accounts cannot be the same");
        }

        Account sourceAccount = accountRepository.findByAccountNumber(request.getSourceAccountNumber().trim())
                .orElseThrow(() -> new AccountNotFoundException("Source account not found: " + request.getSourceAccountNumber()));

        accountRepository.findByAccountNumber(request.getDestinationAccountNumber().trim())
                .orElseThrow(() -> new AccountNotFoundException("Destination account not found: " + request.getDestinationAccountNumber()));

        BigDecimal fee = FeeCalculator.calculateTransactionFee(request.getAmount());
        BigDecimal billedAmount = FeeCalculator.calculateBilledAmount(request.getAmount(), fee);

        if (sourceAccount.getBalance().compareTo(billedAmount) < 0) {
            throw new InvalidTransferException("Insufficient funds: available balance is " + sourceAccount.getBalance()
                    + ", required billed amount is " + billedAmount);
        }

        TransferOtpSession session = otpService.createTransferOtpSession(authenticatedUser, request, fee, billedAmount);

        return InitiateTransferResponse.builder()
                .sessionId(session.getSessionId())
                .sourceAccountNumber(session.getSourceAccountNumber())
                .destinationAccountNumber(session.getDestinationAccountNumber())
                .amount(session.getAmount())
                .transactionFee(fee)
                .billedAmount(billedAmount)
                .expiresAt(session.getExpiresAt())
                .message("OTP has been sent to your registered channel. Please authorize the transfer within 5 minutes.")
                .build();
    }

    @Override
    @Transactional
    public TransferResponse authorizeTransfer(AuthorizeTransferRequest request, User authenticatedUser) {
        String username = (authenticatedUser != null) ? authenticatedUser.getUsername() : null;
        log.info("Authorizing transfer session {} for user {}", request.getSessionId(), username);

        // Verify OTP and consume session
        TransferOtpSession session = otpService.validateAndConsumeOtp(request.getSessionId(), request.getOtpCode(), username);

        // Build internal transfer request from verified session
        TransferRequest transferRequest = TransferRequest.builder()
                .sourceAccountNumber(session.getSourceAccountNumber())
                .destinationAccountNumber(session.getDestinationAccountNumber())
                .amount(session.getAmount())
                .description(session.getDescription())
                .transactionReference(session.getTransactionReference())
                .build();

        return executeTransfer(transferRequest);
    }

    /**
     * Internal atomic transfer execution with deterministic pessimistic locking.
     */
    private TransferResponse executeTransfer(TransferRequest request) {
        log.info("Executing transfer from account {} to account {} for amount {}",
                request.getSourceAccountNumber(), request.getDestinationAccountNumber(), request.getAmount());

        if (request.getSourceAccountNumber().trim().equalsIgnoreCase(request.getDestinationAccountNumber().trim())) {
            throw new InvalidTransferException("Source and destination accounts cannot be the same");
        }

        String reference = (request.getTransactionReference() != null && !request.getTransactionReference().trim().isEmpty())
                ? request.getTransactionReference().trim()
                : generateReference();

        if (transactionRepository.existsByTransactionReference(reference)) {
            throw new InvalidTransferException("Transaction reference already exists: " + reference);
        }

        BigDecimal fee = FeeCalculator.calculateTransactionFee(request.getAmount());
        BigDecimal billedAmount = FeeCalculator.calculateBilledAmount(request.getAmount(), fee);

        // Prevent deadlocks by acquiring pessimistic locks in consistent order
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

        // Check sufficient funds
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

        // Atomic debit and credit
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
