package com.dotlabs.moneytransfer.service;

import com.dotlabs.moneytransfer.dto.response.DailySummaryResponse;
import com.dotlabs.moneytransfer.entity.DailyTransactionSummary;
import com.dotlabs.moneytransfer.entity.Transaction;
import com.dotlabs.moneytransfer.enums.TransactionStatus;
import com.dotlabs.moneytransfer.repository.DailyTransactionSummaryRepository;
import com.dotlabs.moneytransfer.repository.TransactionRepository;
import com.dotlabs.moneytransfer.service.impl.SummaryServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SummaryServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private DailyTransactionSummaryRepository summaryRepository;

    @InjectMocks
    private SummaryServiceImpl summaryService;

    @Test
    @DisplayName("Should aggregate daily transaction summary accurately")
    void testGetDailySummary() {
        LocalDate testDate = LocalDate.of(2026, 8, 28);

        Transaction tx1 = Transaction.builder()
                .amount(new BigDecimal("10000.00"))
                .transactionFee(new BigDecimal("50.00"))
                .commission(new BigDecimal("10.00"))
                .status(TransactionStatus.SUCCESSFUL)
                .dateCreated(testDate.atTime(10, 0))
                .build();

        Transaction tx2 = Transaction.builder()
                .amount(new BigDecimal("5000.00"))
                .transactionFee(new BigDecimal("25.00"))
                .commission(new BigDecimal("5.00"))
                .status(TransactionStatus.SUCCESSFUL)
                .dateCreated(testDate.atTime(11, 0))
                .build();

        Transaction tx3 = Transaction.builder()
                .amount(new BigDecimal("50000.00"))
                .transactionFee(new BigDecimal("100.00"))
                .status(TransactionStatus.INSUFFICIENT_FUNDS)
                .dateCreated(testDate.atTime(12, 0))
                .build();

        Transaction tx4 = Transaction.builder()
                .amount(new BigDecimal("100.00"))
                .transactionFee(new BigDecimal("0.50"))
                .status(TransactionStatus.FAILED)
                .dateCreated(testDate.atTime(13, 0))
                .build();

        when(transactionRepository.findByDateCreatedBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(tx1, tx2, tx3, tx4));

        DailySummaryResponse response = summaryService.getDailySummary(testDate);

        assertThat(response).isNotNull();
        assertThat(response.getSummaryDate()).isEqualTo(testDate);
        assertThat(response.getTotalTransactions()).isEqualTo(4);
        assertThat(response.getSuccessfulTransactions()).isEqualTo(2);
        assertThat(response.getInsufficientFundsTransactions()).isEqualTo(1);
        assertThat(response.getFailedTransactions()).isEqualTo(1);
        assertThat(response.getTotalSuccessfulVolume()).isEqualByComparingTo("15000.00");
        assertThat(response.getTotalFees()).isEqualByComparingTo("75.00");
        assertThat(response.getTotalCommission()).isEqualByComparingTo("15.00");
        assertThat(response.getStatusBreakdown()).containsEntry("SUCCESSFUL", 2L);
        assertThat(response.getStatusBreakdown()).containsEntry("INSUFFICIENT_FUNDS", 1L);
        assertThat(response.getStatusBreakdown()).containsEntry("FAILED", 1L);
    }

    @Test
    @DisplayName("Should generate and persist daily transaction summary entity")
    void testGenerateAndPersistDailySummary() {
        LocalDate testDate = LocalDate.of(2026, 8, 27);

        when(transactionRepository.findByDateCreatedBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(summaryRepository.findBySummaryDate(testDate)).thenReturn(Optional.empty());
        when(summaryRepository.save(any(DailyTransactionSummary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DailyTransactionSummary saved = summaryService.generateAndPersistDailySummary(testDate);

        assertThat(saved).isNotNull();
        assertThat(saved.getSummaryDate()).isEqualTo(testDate);
        verify(summaryRepository).save(any(DailyTransactionSummary.class));
    }
}
