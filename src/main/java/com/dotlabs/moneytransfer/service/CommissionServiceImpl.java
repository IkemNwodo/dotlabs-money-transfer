package com.dotlabs.moneytransfer.service;

import com.dotlabs.moneytransfer.dto.response.CommissionAnalysisResponse;
import com.dotlabs.moneytransfer.entity.Transaction;
import com.dotlabs.moneytransfer.enums.TransactionStatus;
import com.dotlabs.moneytransfer.repository.TransactionRepository;
import com.dotlabs.moneytransfer.util.FeeCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommissionServiceImpl implements CommissionService {

    private final TransactionRepository transactionRepository;

    @Override
    @Transactional
    public CommissionAnalysisResponse runCommissionAnalysis() {
        long startTime = System.currentTimeMillis();
        log.info("Starting Commission Analysis job execution...");

        // Retrieve all successful transactions not yet evaluated
        List<Transaction> successfulTransactions = transactionRepository.findByStatusAndCommissionWorthyIsNull(TransactionStatus.SUCCESSFUL);
        
        BigDecimal totalCommissionCalculated = BigDecimal.ZERO;
        int commissionWorthyCount = 0;
        LocalDateTime now = LocalDateTime.now();

        for (Transaction tx : successfulTransactions) {
            BigDecimal commission = FeeCalculator.calculateCommission(tx.getTransactionFee());
            tx.setCommissionWorthy(true);
            tx.setCommission(commission);
            tx.setCommissionProcessedAt(now);

            totalCommissionCalculated = totalCommissionCalculated.add(commission);
            commissionWorthyCount++;
        }

        if (!successfulTransactions.isEmpty()) {
            transactionRepository.saveAll(successfulTransactions);
        }

        // Also evaluate non-successful transactions that haven't been marked yet
        List<Transaction> insufficientFundTxs = transactionRepository.findByStatusAndCommissionWorthyIsNull(TransactionStatus.INSUFFICIENT_FUNDS);
        for (Transaction tx : insufficientFundTxs) {
            tx.setCommissionWorthy(false);
            tx.setCommission(BigDecimal.ZERO);
            tx.setCommissionProcessedAt(now);
        }
        if (!insufficientFundTxs.isEmpty()) {
            transactionRepository.saveAll(insufficientFundTxs);
        }

        List<Transaction> failedTxs = transactionRepository.findByStatusAndCommissionWorthyIsNull(TransactionStatus.FAILED);
        for (Transaction tx : failedTxs) {
            tx.setCommissionWorthy(false);
            tx.setCommission(BigDecimal.ZERO);
            tx.setCommissionProcessedAt(now);
        }
        if (!failedTxs.isEmpty()) {
            transactionRepository.saveAll(failedTxs);
        }

        int totalAnalyzed = successfulTransactions.size() + insufficientFundTxs.size() + failedTxs.size();
        long duration = System.currentTimeMillis() - startTime;

        log.info("Commission Analysis completed. Analyzed: {}, Commission-Worthy: {}, Total Commission: {}, Duration: {}ms",
                totalAnalyzed, commissionWorthyCount, totalCommissionCalculated, duration);

        return CommissionAnalysisResponse.builder()
                .totalAnalyzed(totalAnalyzed)
                .commissionWorthyUpdated(commissionWorthyCount)
                .totalCommissionCalculated(totalCommissionCalculated)
                .executionTimestamp(now)
                .executionDurationMs(duration)
                .build();
    }
}
