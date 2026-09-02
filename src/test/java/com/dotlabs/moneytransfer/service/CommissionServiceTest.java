package com.dotlabs.moneytransfer.service;

import com.dotlabs.moneytransfer.dto.response.CommissionAnalysisResponse;
import com.dotlabs.moneytransfer.enums.TransactionStatus;
import com.dotlabs.moneytransfer.repository.TransactionRepository;
import com.dotlabs.moneytransfer.service.impl.CommissionServiceImpl;
import com.dotlabs.moneytransfer.util.FeeCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommissionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private CommissionServiceImpl commissionService;

    @Test
    @DisplayName("Should evaluate successful transactions and assign 20% commission on fee via DB bulk operations")
    void testCommissionAnalysisJob() {
        when(transactionRepository.calculateTotalCommissionForPending(
                eq(TransactionStatus.SUCCESSFUL),
                eq(FeeCalculator.COMMISSION_PERCENTAGE)
        )).thenReturn(new BigDecimal("21.00"));

        when(transactionRepository.evaluateCommissionForSuccessfulTransactions(
                eq(TransactionStatus.SUCCESSFUL),
                eq(FeeCalculator.COMMISSION_PERCENTAGE),
                any(LocalDateTime.class)
        )).thenReturn(2);

        when(transactionRepository.markNonSuccessfulTransactionsNonCommissionWorthy(
                eq(TransactionStatus.SUCCESSFUL),
                any(LocalDateTime.class)
        )).thenReturn(1);

        CommissionAnalysisResponse response = commissionService.runCommissionAnalysis();

        assertThat(response).isNotNull();
        assertThat(response.getTotalAnalyzed()).isEqualTo(3);
        assertThat(response.getCommissionWorthyUpdated()).isEqualTo(2);
        assertThat(response.getTotalCommissionCalculated()).isEqualByComparingTo("21.00");
        assertThat(response.getExecutionDurationMs()).isGreaterThanOrEqualTo(0);

        verify(transactionRepository).calculateTotalCommissionForPending(
                eq(TransactionStatus.SUCCESSFUL),
                eq(FeeCalculator.COMMISSION_PERCENTAGE)
        );
        verify(transactionRepository).evaluateCommissionForSuccessfulTransactions(
                eq(TransactionStatus.SUCCESSFUL),
                eq(FeeCalculator.COMMISSION_PERCENTAGE),
                any(LocalDateTime.class)
        );
        verify(transactionRepository).markNonSuccessfulTransactionsNonCommissionWorthy(
                eq(TransactionStatus.SUCCESSFUL),
                any(LocalDateTime.class)
        );
    }
}
