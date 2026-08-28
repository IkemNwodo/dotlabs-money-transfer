package com.dotlabs.moneytransfer.service;

import com.dotlabs.moneytransfer.dto.response.DailySummaryResponse;
import com.dotlabs.moneytransfer.entity.DailyTransactionSummary;
import com.dotlabs.moneytransfer.entity.Transaction;
import com.dotlabs.moneytransfer.enums.TransactionStatus;
import com.dotlabs.moneytransfer.repository.DailyTransactionSummaryRepository;
import com.dotlabs.moneytransfer.repository.TransactionRepository;
import com.dotlabs.moneytransfer.util.FeeCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SummaryServiceImpl implements SummaryService {

    private final TransactionRepository transactionRepository;
    private final DailyTransactionSummaryRepository summaryRepository;

    @Override
    @Transactional(readOnly = true)
    public DailySummaryResponse getDailySummary(LocalDate date) {
        LocalDate queryDate = (date != null) ? date : LocalDate.now();
        log.info("Calculating daily transaction summary for date: {}", queryDate);

        LocalDateTime startOfDay = queryDate.atStartOfDay();
        LocalDateTime endOfDay = queryDate.atTime(LocalTime.MAX);

        List<Transaction> transactions = transactionRepository.findByDateCreatedBetween(startOfDay, endOfDay);

        long totalTransactions = transactions.size();
        long successfulCount = 0;
        long insufficientFundsCount = 0;
        long failedCount = 0;

        BigDecimal totalVolume = BigDecimal.ZERO;
        BigDecimal totalFees = BigDecimal.ZERO;
        BigDecimal totalCommission = BigDecimal.ZERO;

        Map<String, Long> statusBreakdown = new HashMap<>();

        for (Transaction tx : transactions) {
            String statusKey = tx.getStatus().name();
            statusBreakdown.put(statusKey, statusBreakdown.getOrDefault(statusKey, 0L) + 1);

            if (tx.getStatus() == TransactionStatus.SUCCESSFUL) {
                successfulCount++;
                totalVolume = totalVolume.add(tx.getAmount());
                totalFees = totalFees.add(tx.getTransactionFee());
                
                BigDecimal commission = (tx.getCommission() != null)
                        ? tx.getCommission()
                        : FeeCalculator.calculateCommission(tx.getTransactionFee());
                totalCommission = totalCommission.add(commission);
            } else if (tx.getStatus() == TransactionStatus.INSUFFICIENT_FUNDS) {
                insufficientFundsCount++;
            } else {
                failedCount++;
            }
        }

        return DailySummaryResponse.builder()
                .summaryDate(queryDate)
                .totalTransactions(totalTransactions)
                .successfulTransactions(successfulCount)
                .insufficientFundsTransactions(insufficientFundsCount)
                .failedTransactions(failedCount)
                .totalSuccessfulVolume(totalVolume)
                .totalFees(totalFees)
                .totalCommission(totalCommission)
                .statusBreakdown(statusBreakdown)
                .build();
    }

    @Override
    @Transactional
    public DailyTransactionSummary generateAndPersistDailySummary(LocalDate date) {
        LocalDate summaryDate = (date != null) ? date : LocalDate.now().minusDays(1);
        log.info("Generating and persisting daily summary for date: {}", summaryDate);

        DailySummaryResponse response = getDailySummary(summaryDate);

        DailyTransactionSummary summary = summaryRepository.findBySummaryDate(summaryDate)
                .orElseGet(() -> DailyTransactionSummary.builder().summaryDate(summaryDate).build());

        summary.setTotalTransactions(response.getTotalTransactions());
        summary.setSuccessfulTransactions(response.getSuccessfulTransactions());
        summary.setInsufficientFundsTransactions(response.getInsufficientFundsTransactions());
        summary.setFailedTransactions(response.getFailedTransactions());
        summary.setTotalVolume(response.getTotalSuccessfulVolume());
        summary.setTotalFees(response.getTotalFees());
        summary.setTotalCommission(response.getTotalCommission());

        DailyTransactionSummary saved = summaryRepository.save(summary);
        log.info("Persisted daily summary for {}: {} transactions, volume {}",
                summaryDate, saved.getTotalTransactions(), saved.getTotalVolume());
        return saved;
    }
}
