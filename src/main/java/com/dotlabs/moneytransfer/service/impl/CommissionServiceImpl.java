package com.dotlabs.moneytransfer.service.impl;

import com.dotlabs.moneytransfer.dto.response.CommissionAnalysisResponse;
import com.dotlabs.moneytransfer.enums.TransactionStatus;
import com.dotlabs.moneytransfer.repository.TransactionRepository;
import com.dotlabs.moneytransfer.service.CommissionService;
import com.dotlabs.moneytransfer.util.FeeCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommissionServiceImpl implements CommissionService {

    private final TransactionRepository transactionRepository;

    @Override
    @Transactional
    public CommissionAnalysisResponse runCommissionAnalysis() {
        long startTime = System.currentTimeMillis();
        log.info("Starting Commission Analysis job execution in database...");

        LocalDateTime now = LocalDateTime.now();

        // 1. Calculate total commission to be evaluated in DB
        BigDecimal totalCommissionCalculated = transactionRepository.calculateTotalCommissionForPending(
                TransactionStatus.SUCCESSFUL,
                FeeCalculator.COMMISSION_PERCENTAGE
        );

        // 2. Bulk update all SUCCESSFUL transactions in the database (20% commission on transaction fee)
        int commissionWorthyCount = transactionRepository.evaluateCommissionForSuccessfulTransactions(
                TransactionStatus.SUCCESSFUL,
                FeeCalculator.COMMISSION_PERCENTAGE,
                now
        );

        // 3. Bulk mark all non-successful transactions (INSUFFICIENT_FUNDS, FAILED) as non-commission-worthy
        int nonCommissionCount = transactionRepository.markNonSuccessfulTransactionsNonCommissionWorthy(
                TransactionStatus.SUCCESSFUL,
                now
        );

        int totalAnalyzed = commissionWorthyCount + nonCommissionCount;
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
