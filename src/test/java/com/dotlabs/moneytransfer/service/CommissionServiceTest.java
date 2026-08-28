package com.dotlabs.moneytransfer.service;

import com.dotlabs.moneytransfer.dto.response.CommissionAnalysisResponse;
import com.dotlabs.moneytransfer.entity.Transaction;
import com.dotlabs.moneytransfer.enums.TransactionStatus;
import com.dotlabs.moneytransfer.repository.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommissionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private CommissionServiceImpl commissionService;

    @Test
    @DisplayName("Should evaluate successful transactions and assign 20% commission on fee")
    void testCommissionAnalysisJob() {
        Transaction tx1 = Transaction.builder()
                .id(1L)
                .transactionReference("TX-1")
                .amount(new BigDecimal("1000.00"))
                .transactionFee(new BigDecimal("5.00")) // 20% = 1.00
                .status(TransactionStatus.SUCCESSFUL)
                .commissionWorthy(null)
                .build();

        Transaction tx2 = Transaction.builder()
                .id(2L)
                .transactionReference("TX-2")
                .amount(new BigDecimal("20000.00"))
                .transactionFee(new BigDecimal("100.00")) // 20% = 20.00
                .status(TransactionStatus.SUCCESSFUL)
                .commissionWorthy(null)
                .build();

        Transaction failedTx = Transaction.builder()
                .id(3L)
                .transactionReference("TX-3")
                .amount(new BigDecimal("5000.00"))
                .transactionFee(new BigDecimal("25.00"))
                .status(TransactionStatus.INSUFFICIENT_FUNDS)
                .commissionWorthy(null)
                .build();

        when(transactionRepository.findByStatusAndCommissionWorthyIsNull(TransactionStatus.SUCCESSFUL))
                .thenReturn(List.of(tx1, tx2));
        when(transactionRepository.findByStatusAndCommissionWorthyIsNull(TransactionStatus.INSUFFICIENT_FUNDS))
                .thenReturn(List.of(failedTx));
        when(transactionRepository.findByStatusAndCommissionWorthyIsNull(TransactionStatus.FAILED))
                .thenReturn(List.of());

        CommissionAnalysisResponse response = commissionService.runCommissionAnalysis();

        assertThat(response).isNotNull();
        assertThat(response.getTotalAnalyzed()).isEqualTo(3);
        assertThat(response.getCommissionWorthyUpdated()).isEqualTo(2);
        assertThat(response.getTotalCommissionCalculated()).isEqualByComparingTo("21.00"); // 1.00 + 20.00

        // Check modified entities
        assertThat(tx1.getCommissionWorthy()).isTrue();
        assertThat(tx1.getCommission()).isEqualByComparingTo("1.00");
        assertThat(tx1.getCommissionProcessedAt()).isNotNull();

        assertThat(tx2.getCommissionWorthy()).isTrue();
        assertThat(tx2.getCommission()).isEqualByComparingTo("20.00");

        assertThat(failedTx.getCommissionWorthy()).isFalse();
        assertThat(failedTx.getCommission()).isEqualByComparingTo("0.00");

        verify(transactionRepository).saveAll(List.of(tx1, tx2));
        verify(transactionRepository).saveAll(List.of(failedTx));
    }
}
